package com.enovak.lotrmoremobs.siege.banner;

import com.enovak.lotrmoremobs.siege.SiegeRegistry;
import com.enovak.lotrmoremobs.siege.gate.GateLeaf;
import com.enovak.lotrmoremobs.siege.gate.GatePartData;
import com.enovak.lotrmoremobs.siege.gate.GateStructureValidator;
import com.enovak.lotrmoremobs.siege.tile.TileEntitySiegeGate;
import cpw.mods.fml.common.FMLLog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lotr.common.entity.item.LOTREntityBanner;
import lotr.common.entity.item.LOTREntityBannerWall;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

/**
 * Durable sidecar for native LOTR banners attached to Siege Gate source
 * blocks.
 *
 * <p>The live LOTR banner entity is deliberately absent while the gate is a
 * finalized moving structure. Only inert entity NBT is retained here. That
 * keeps native appearance/ownership data available for restoration without
 * allowing a moving {@link LOTREntityBanner} to participate in LOTR banner
 * protection scans.</p>
 *
 * <p>This is intentionally separate from {@code SiegeGateOwnershipData}. The
 * gate ownership/removal journal remains the sole authority for block
 * restoration; this sidecar waits for those support blocks to be restored and
 * only then recreates the native banner entity.</p>
 */
public final class SiegeGateBannerAttachmentData extends WorldSavedData {

    public static final String DATA_NAME =
            "lotrmoremobs_siege_gate_banner_attachments";
    private static final int FORMAT_VERSION = 1;
    private static final int MAX_GATE_RECORDS = 4096;
    private static final int MAX_ATTACHMENTS_PER_GATE =
            GateStructureValidator.MAX_GATE_PARTS * 4;
    private static final int MAX_RESTORES_PER_TICK = 16;

    private static final int TAG_INT = 3;
    private static final int TAG_STRING = 8;
    private static final int TAG_LIST = 9;
    private static final int TAG_COMPOUND = 10;

    private static final String NBT_FORMAT_VERSION = "FormatVersion";
    private static final String NBT_GATES = "Gates";
    private static final String NBT_GATE_UUID = "GateUUID";
    private static final String NBT_DIMENSION = "Dimension";
    private static final String NBT_CONTROLLER_X = "ControllerX";
    private static final String NBT_CONTROLLER_Y = "ControllerY";
    private static final String NBT_CONTROLLER_Z = "ControllerZ";
    private static final String NBT_STATE = "State";
    private static final String NBT_ATTACHMENTS = "Attachments";
    private static final String NBT_KIND = "Kind";
    private static final String NBT_RELATIVE_SUPPORT_X = "RelativeSupportX";
    private static final String NBT_RELATIVE_SUPPORT_Y = "RelativeSupportY";
    private static final String NBT_RELATIVE_SUPPORT_Z = "RelativeSupportZ";
    private static final String NBT_LEAF = "Leaf";
    private static final String NBT_ENTITY_UUID = "EntityUUID";
    private static final String NBT_ENTITY = "EntityNBT";

    private final Map<UUID, GateRecord> recordsByGateUuid =
            new HashMap<UUID, GateRecord>();
    private boolean readOnlyDueToInvalidData;
    private int boundDimension = Integer.MIN_VALUE;

    public SiegeGateBannerAttachmentData(String name) {
        super(name);
    }

    public static SiegeGateBannerAttachmentData get(
            World world,
            boolean create
    ) {
        if (world == null || world.isRemote || world.perWorldStorage == null) {
            return null;
        }

        SiegeGateBannerAttachmentData data =
                (SiegeGateBannerAttachmentData)world.perWorldStorage.loadData(
                        SiegeGateBannerAttachmentData.class,
                        DATA_NAME
                );

        if (data == null && create) {
            data = new SiegeGateBannerAttachmentData(DATA_NAME);
            world.perWorldStorage.setData(DATA_NAME, data);
            data.markDirty();
        }

        if (data != null) {
            data.bindToDimension(world.provider.dimensionId);
        }

        return data;
    }

    /** Captures/removes native banners before any source block is converted. */
    public static PrepareResult prepareFinalization(
            World world,
            TileEntitySiegeGate controller,
            Collection<GatePartData> parts
    ) {
        if (world == null
                || world.isRemote
                || controller == null
                || parts == null) {
            return PrepareResult.failure(
                    "Gate banner capture could not start safely."
            );
        }

        SiegeGateBannerAttachmentData data = get(world, true);
        if (data == null) {
            return PrepareResult.failure(
                    "Gate banner attachment storage is unavailable."
            );
        }

        return data.prepareFinalizationInternal(world, controller, parts);
    }

    public static void commitFinalization(World world, UUID gateUuid) {
        SiegeGateBannerAttachmentData data = get(world, false);
        if (data != null) {
            data.commitFinalizationInternal(gateUuid);
        }
    }

    public static void rollbackFinalization(World world, UUID gateUuid) {
        SiegeGateBannerAttachmentData data = get(world, false);
        if (data != null) {
            data.rollbackFinalizationInternal(world, gateUuid);
        }
    }

    public static boolean beginRestoration(World world, UUID gateUuid) {
        SiegeGateBannerAttachmentData data = get(world, false);
        if (data == null) {
            return true;
        }
        return data.beginRestorationInternal(gateUuid);
    }

    public static void abortRestoration(World world, UUID gateUuid) {
        SiegeGateBannerAttachmentData data = get(world, false);
        if (data != null) {
            data.abortRestorationInternal(gateUuid);
        }
    }

    /** Conservative edit guard until structural edits are attachment-aware. */
    public static boolean hasAttachments(World world, UUID gateUuid) {
        if (gateUuid == null) {
            return false;
        }
        SiegeGateBannerAttachmentData data = get(world, false);
        if (data == null) {
            return false;
        }
        synchronized (data) {
            if (data.readOnlyDueToInvalidData) {
                return true;
            }
            GateRecord record = data.recordsByGateUuid.get(gateUuid);
            return record != null && !record.attachments.isEmpty();
        }
    }

    /** Called after the ordinary Siege Gate block journal each server tick. */
    public static void process(World world) {
        SiegeGateBannerAttachmentData data = get(world, false);
        if (data != null) {
            data.processInternal(world);
        }
    }

    private synchronized PrepareResult prepareFinalizationInternal(
            World world,
            TileEntitySiegeGate controller,
            Collection<GatePartData> parts
    ) {
        if (readOnlyDueToInvalidData) {
            return PrepareResult.failure(
                    "Gate banner attachment data is read-only; manual recovery is required."
            );
        }

        UUID gateUuid = controller.getGateUuid();
        if (gateUuid == null) {
            return PrepareResult.failure(
                    "The Siege Gate has no stable identity for banner capture."
            );
        }
        if (recordsByGateUuid.containsKey(gateUuid)) {
            return PrepareResult.failure(
                    "This Siege Gate already has pending banner attachment data."
            );
        }
        if (recordsByGateUuid.size() >= MAX_GATE_RECORDS) {
            return PrepareResult.failure(
                    "Siege Gate banner attachment storage is full."
            );
        }

        Map<SupportPosition, GateLeaf> selectedSupports =
                new HashMap<SupportPosition, GateLeaf>();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (GatePartData part : parts) {
            if (part == null
                    || !part.hasValidAbsolutePosition(
                            controller.xCoord,
                            controller.yCoord,
                            controller.zCoord
                    )) {
                return PrepareResult.failure(
                        "Gate banner capture found an invalid gate-part position."
                );
            }

            int x = controller.xCoord + part.getRelativeX();
            int y = controller.yCoord + part.getRelativeY();
            int z = controller.zCoord + part.getRelativeZ();
            selectedSupports.put(
                    new SupportPosition(x, y, z),
                    part.getLeaf()
            );

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        if (selectedSupports.isEmpty()) {
            return PrepareResult.success(false);
        }

        AxisAlignedBB searchBounds = AxisAlignedBB.getBoundingBox(
                minX - 2.0D,
                minY - 2.0D,
                minZ - 2.0D,
                maxX + 3.0D,
                maxY + 5.0D,
                maxZ + 3.0D
        );

        List<Attachment> attachments = new ArrayList<Attachment>();
        List<Entity> entitiesToRemove = new ArrayList<Entity>();

        String captureError = captureStandingBanners(
                world,
                controller,
                selectedSupports,
                searchBounds,
                attachments,
                entitiesToRemove
        );
        if (captureError == null) {
            captureError = captureWallBanners(
                    world,
                    controller,
                    selectedSupports,
                    searchBounds,
                    attachments,
                    entitiesToRemove
            );
        }
        if (captureError != null) {
            return PrepareResult.failure(captureError);
        }
        if (attachments.isEmpty()) {
            return PrepareResult.success(false);
        }
        if (attachments.size() > MAX_ATTACHMENTS_PER_GATE) {
            return PrepareResult.failure(
                    "Too many LOTR banner attachments are connected to this gate."
            );
        }

        GateRecord record = new GateRecord(
                gateUuid,
                world.provider.dimensionId,
                controller.xCoord,
                controller.yCoord,
                controller.zCoord,
                RecordState.PREPARED,
                attachments
        );
        recordsByGateUuid.put(gateUuid, record);
        markDirty();

        try {
            for (Entity entity : entitiesToRemove) {
                if (entity != null && !entity.isDead) {
                    /* Avoid native damage/onBroken/drop paths. */
                    world.removeEntity(entity);
                }
            }
        } catch (RuntimeException exception) {
            record.state = RecordState.RESTORING;
            markDirty();
            restoreRecord(world, record, Integer.MAX_VALUE);
            return PrepareResult.failure(
                    "Gate banner capture failed; captured banners were queued for restoration."
            );
        }

        return PrepareResult.success(true);
    }

    private String captureStandingBanners(
            World world,
            TileEntitySiegeGate controller,
            Map<SupportPosition, GateLeaf> selectedSupports,
            AxisAlignedBB searchBounds,
            List<Attachment> attachments,
            List<Entity> entitiesToRemove
    ) {
        @SuppressWarnings("unchecked")
        List<LOTREntityBanner> banners = world.getEntitiesWithinAABB(
                LOTREntityBanner.class,
                searchBounds
        );

        for (LOTREntityBanner banner : banners) {
            if (banner == null || banner.isDead) {
                continue;
            }

            SupportPosition support = new SupportPosition(
                    MathHelper.floor_double(banner.posX),
                    MathHelper.floor_double(banner.boundingBox.minY) - 1,
                    MathHelper.floor_double(banner.posZ)
            );
            GateLeaf leaf = selectedSupports.get(support);
            if (leaf == null) {
                continue;
            }
            if (leaf.isSplitCenter()) {
                return "A LOTR banner is supported by a split-center gate block; "
                        + "move the banner or assign that block to one leaf before finalizing.";
            }

            Attachment attachment = captureAttachment(
                    banner,
                    AttachmentKind.STANDING,
                    support,
                    leaf,
                    controller
            );
            if (attachment == null) {
                return "A LOTR standing banner could not be captured safely.";
            }
            attachments.add(attachment);
            entitiesToRemove.add(banner);
        }

        return null;
    }

    private String captureWallBanners(
            World world,
            TileEntitySiegeGate controller,
            Map<SupportPosition, GateLeaf> selectedSupports,
            AxisAlignedBB searchBounds,
            List<Attachment> attachments,
            List<Entity> entitiesToRemove
    ) {
        @SuppressWarnings("unchecked")
        List<LOTREntityBannerWall> banners = world.getEntitiesWithinAABB(
                LOTREntityBannerWall.class,
                searchBounds
        );

        for (LOTREntityBannerWall banner : banners) {
            if (banner == null || banner.isDead) {
                continue;
            }

            NBTTagCompound entityNbt = new NBTTagCompound();
            try {
                banner.writeToNBT(entityNbt);
            } catch (RuntimeException exception) {
                return "A LOTR wall banner could not be captured safely.";
            }

            /*
             * EntityHanging persists its authoritative support block as
             * TileX/TileY/TileZ. Read that detached snapshot instead of
             * depending on inherited MCP field visibility.
             */
            if (!entityNbt.hasKey("TileX", TAG_INT)
                    || !entityNbt.hasKey("TileY", TAG_INT)
                    || !entityNbt.hasKey("TileZ", TAG_INT)) {
                return "A LOTR wall banner has no valid hanging anchor.";
            }

            SupportPosition support = new SupportPosition(
                    entityNbt.getInteger("TileX"),
                    entityNbt.getInteger("TileY"),
                    entityNbt.getInteger("TileZ")
            );
            GateLeaf leaf = selectedSupports.get(support);
            if (leaf == null) {
                continue;
            }
            if (leaf.isSplitCenter()) {
                return "A LOTR wall banner is supported by a split-center gate block; "
                        + "move the banner or assign that block to one leaf before finalizing.";
            }

            UUID entityUuid = banner.getUniqueID();
            if (entityUuid == null) {
                return "A LOTR wall banner could not be captured safely.";
            }
            attachments.add(new Attachment(
                    AttachmentKind.WALL,
                    support.x - controller.xCoord,
                    support.y - controller.yCoord,
                    support.z - controller.zCoord,
                    leaf,
                    entityUuid,
                    entityNbt,
                    false
            ));
            entitiesToRemove.add(banner);
        }

        return null;
    }

    private Attachment captureAttachment(
            Entity entity,
            AttachmentKind kind,
            SupportPosition support,
            GateLeaf leaf,
            TileEntitySiegeGate controller
    ) {
        try {
            NBTTagCompound entityNbt = new NBTTagCompound();
            entity.writeToNBT(entityNbt);
            UUID entityUuid = entity.getUniqueID();
            if (entityUuid == null) {
                return null;
            }

            return new Attachment(
                    kind,
                    support.x - controller.xCoord,
                    support.y - controller.yCoord,
                    support.z - controller.zCoord,
                    leaf,
                    entityUuid,
                    entityNbt,
                    false
            );
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private synchronized void commitFinalizationInternal(UUID gateUuid) {
        if (gateUuid == null || readOnlyDueToInvalidData) {
            return;
        }
        GateRecord record = recordsByGateUuid.get(gateUuid);
        if (record != null && record.state == RecordState.PREPARED) {
            record.state = RecordState.ATTACHED;
            markDirty();
        }
    }

    private synchronized void rollbackFinalizationInternal(
            World world,
            UUID gateUuid
    ) {
        if (world == null || gateUuid == null || readOnlyDueToInvalidData) {
            return;
        }
        GateRecord record = recordsByGateUuid.get(gateUuid);
        if (record == null) {
            return;
        }
        record.state = RecordState.RESTORING;
        markDirty();
        restoreRecord(world, record, Integer.MAX_VALUE);
    }

    private synchronized boolean beginRestorationInternal(UUID gateUuid) {
        if (gateUuid == null) {
            return true;
        }
        if (readOnlyDueToInvalidData) {
            return false;
        }
        GateRecord record = recordsByGateUuid.get(gateUuid);
        if (record == null) {
            return true;
        }
        if (record.state == RecordState.PREPARED) {
            return false;
        }
        if (record.state != RecordState.RESTORING) {
            record.state = RecordState.RESTORING;
            markDirty();
        }
        return true;
    }

    private synchronized void abortRestorationInternal(UUID gateUuid) {
        if (gateUuid == null || readOnlyDueToInvalidData) {
            return;
        }
        GateRecord record = recordsByGateUuid.get(gateUuid);
        if (record != null && record.state == RecordState.RESTORING) {
            record.state = RecordState.ATTACHED;
            for (Attachment attachment : record.attachments) {
                attachment.restored = false;
            }
            markDirty();
        }
    }

    private synchronized void processInternal(World world) {
        if (world == null || world.isRemote || readOnlyDueToInvalidData) {
            return;
        }

        int remainingRestores = MAX_RESTORES_PER_TICK;
        List<GateRecord> records =
                new ArrayList<GateRecord>(recordsByGateUuid.values());

        for (GateRecord record : records) {
            if (record.dimension != world.provider.dimensionId) {
                rejectLoadedData(
                        "banner attachment record dimension does not match storage"
                );
                return;
            }

            if (record.state == RecordState.PREPARED) {
                TileEntity tileEntity = world.blockExists(
                        record.controllerX,
                        record.controllerY,
                        record.controllerZ
                )
                        ? world.getTileEntity(
                        record.controllerX,
                        record.controllerY,
                        record.controllerZ
                )
                        : null;

                if (tileEntity instanceof TileEntitySiegeGate) {
                    TileEntitySiegeGate gate = (TileEntitySiegeGate)tileEntity;
                    if (record.gateUuid.equals(gate.getExistingGateUuid())
                            && gate.isFinalized()) {
                        record.state = RecordState.ATTACHED;
                        markDirty();
                        continue;
                    }
                }

                if (hasAnyGatePartSupport(world, record)) {
                    continue;
                }

                record.state = RecordState.RESTORING;
                markDirty();
            }

            if (record.state == RecordState.RESTORING
                    && remainingRestores > 0) {
                int used = restoreRecord(
                        world,
                        record,
                        remainingRestores
                );
                remainingRestores -= used;
            }

            if (remainingRestores <= 0) {
                break;
            }
        }
    }

    private boolean hasAnyGatePartSupport(World world, GateRecord record) {
        for (Attachment attachment : record.attachments) {
            int x = record.controllerX + attachment.relativeSupportX;
            int y = record.controllerY + attachment.relativeSupportY;
            int z = record.controllerZ + attachment.relativeSupportZ;
            if (world.blockExists(x, y, z)
                    && world.getBlock(x, y, z) == SiegeRegistry.gatePart) {
                return true;
            }
        }
        return false;
    }

    /** Returns the number of actual restore attempts consumed. */
    private int restoreRecord(
            World world,
            GateRecord record,
            int budget
    ) {
        int used = 0;
        boolean changed = false;

        for (Attachment attachment : record.attachments) {
            if (attachment.restored) {
                continue;
            }
            if (used >= budget) {
                break;
            }

            RestoreResult result = restoreAttachment(world, record, attachment);
            if (result == RestoreResult.WAIT) {
                continue;
            }

            ++used;
            attachment.restored = true;
            changed = true;
        }

        if (changed) {
            markDirty();
        }

        boolean complete = true;
        for (Attachment attachment : record.attachments) {
            if (!attachment.restored) {
                complete = false;
                break;
            }
        }

        if (complete) {
            recordsByGateUuid.remove(record.gateUuid);
            markDirty();
        }

        return used;
    }

    private RestoreResult restoreAttachment(
            World world,
            GateRecord record,
            Attachment attachment
    ) {
        int supportX = record.controllerX + attachment.relativeSupportX;
        int supportY = record.controllerY + attachment.relativeSupportY;
        int supportZ = record.controllerZ + attachment.relativeSupportZ;

        if (!world.blockExists(supportX, supportY, supportZ)
                || world.getBlock(supportX, supportY, supportZ)
                == SiegeRegistry.gatePart) {
            return RestoreResult.WAIT;
        }

        AxisAlignedBB searchBounds = AxisAlignedBB.getBoundingBox(
                supportX - 2.0D,
                supportY - 3.0D,
                supportZ - 2.0D,
                supportX + 3.0D,
                supportY + 5.0D,
                supportZ + 3.0D
        );

        Class entityClass = attachment.kind == AttachmentKind.STANDING
                ? LOTREntityBanner.class
                : LOTREntityBannerWall.class;
        @SuppressWarnings("unchecked")
        List<Entity> existing = world.getEntitiesWithinAABB(
                entityClass,
                searchBounds
        );
        for (Entity entity : existing) {
            if (entity != null
                    && !entity.isDead
                    && attachment.entityUuid.equals(entity.getUniqueID())) {
                return RestoreResult.RESTORED;
            }
        }

        try {
            Entity restored;
            if (attachment.kind == AttachmentKind.STANDING) {
                if (!World.doesBlockHaveSolidTopSurface(
                        world,
                        supportX,
                        supportY,
                        supportZ
                )) {
                    return RestoreResult.WAIT;
                }
                restored = new LOTREntityBanner(world);
            } else {
                restored = new LOTREntityBannerWall(world);
            }

            restored.readFromNBT(
                    (NBTTagCompound)attachment.entityNbt.copy()
            );

            if (restored instanceof LOTREntityBannerWall
                    && !((LOTREntityBannerWall)restored).onValidSurface()) {
                restored.setDead();
                return RestoreResult.WAIT;
            }

            if (!world.spawnEntityInWorld(restored)) {
                restored.setDead();
                return RestoreResult.WAIT;
            }

            return RestoreResult.RESTORED;
        } catch (RuntimeException exception) {
            FMLLog.warning(
                    "[LOTRMoreMobs] Siege Gate banner restoration at %d,%d,%d "
                            + "will retry after %s.",
                    supportX,
                    supportY,
                    supportZ,
                    exception.getClass().getSimpleName()
            );
            return RestoreResult.WAIT;
        }
    }

    private synchronized void bindToDimension(int dimension) {
        if (boundDimension == dimension) {
            return;
        }
        if (boundDimension != Integer.MIN_VALUE) {
            rejectLoadedData(
                    "one banner attachment data instance was reused across dimensions"
            );
            return;
        }
        for (GateRecord record : recordsByGateUuid.values()) {
            if (record.dimension != dimension) {
                rejectLoadedData(
                        "banner attachment record dimension does not match storage"
                );
                return;
            }
        }
        boundDimension = dimension;
    }

    @Override
    public synchronized void readFromNBT(NBTTagCompound nbt) {
        recordsByGateUuid.clear();
        readOnlyDueToInvalidData = false;
        boundDimension = Integer.MIN_VALUE;

        if (nbt == null
                || !nbt.hasKey(NBT_FORMAT_VERSION, TAG_INT)
                || nbt.getInteger(NBT_FORMAT_VERSION) != FORMAT_VERSION
                || !nbt.hasKey(NBT_GATES, TAG_LIST)) {
            rejectLoadedData("missing or unsupported banner attachment format");
            return;
        }

        NBTTagList gateList = (NBTTagList)nbt.getTag(NBT_GATES);
        if (gateList.tagCount() > 0
                && gateList.func_150303_d() != TAG_COMPOUND) {
            rejectLoadedData("banner attachment Gates contains non-compounds");
            return;
        }
        if (gateList.tagCount() > MAX_GATE_RECORDS) {
            rejectLoadedData("too many banner attachment gate records");
            return;
        }

        for (int i = 0; i < gateList.tagCount(); ++i) {
            NBTTagCompound gateNbt = gateList.getCompoundTagAt(i);
            GateRecord record = readGateRecord(gateNbt);
            if (record == null
                    || recordsByGateUuid.put(record.gateUuid, record) != null) {
                rejectLoadedData("invalid or duplicate banner attachment gate record");
                return;
            }
        }
    }

    @Override
    public synchronized void writeToNBT(NBTTagCompound nbt) {
        if (nbt == null || readOnlyDueToInvalidData) {
            return;
        }

        nbt.setInteger(NBT_FORMAT_VERSION, FORMAT_VERSION);
        NBTTagList gateList = new NBTTagList();

        for (GateRecord record : recordsByGateUuid.values()) {
            NBTTagCompound gateNbt = new NBTTagCompound();
            gateNbt.setString(NBT_GATE_UUID, record.gateUuid.toString());
            gateNbt.setInteger(NBT_DIMENSION, record.dimension);
            gateNbt.setInteger(NBT_CONTROLLER_X, record.controllerX);
            gateNbt.setInteger(NBT_CONTROLLER_Y, record.controllerY);
            gateNbt.setInteger(NBT_CONTROLLER_Z, record.controllerZ);
            gateNbt.setString(NBT_STATE, record.state.name());

            NBTTagList attachmentList = new NBTTagList();
            for (Attachment attachment : record.attachments) {
                NBTTagCompound attachmentNbt = new NBTTagCompound();
                attachmentNbt.setString(NBT_KIND, attachment.kind.name());
                attachmentNbt.setInteger(
                        NBT_RELATIVE_SUPPORT_X,
                        attachment.relativeSupportX
                );
                attachmentNbt.setInteger(
                        NBT_RELATIVE_SUPPORT_Y,
                        attachment.relativeSupportY
                );
                attachmentNbt.setInteger(
                        NBT_RELATIVE_SUPPORT_Z,
                        attachment.relativeSupportZ
                );
                attachmentNbt.setString(NBT_LEAF, attachment.leaf.name());
                attachmentNbt.setString(
                        NBT_ENTITY_UUID,
                        attachment.entityUuid.toString()
                );
                attachmentNbt.setTag(
                        NBT_ENTITY,
                        attachment.entityNbt.copy()
                );
                attachmentList.appendTag(attachmentNbt);
            }

            gateNbt.setTag(NBT_ATTACHMENTS, attachmentList);
            gateList.appendTag(gateNbt);
        }

        nbt.setTag(NBT_GATES, gateList);
    }

    private GateRecord readGateRecord(NBTTagCompound nbt) {
        if (nbt == null
                || !nbt.hasKey(NBT_GATE_UUID, TAG_STRING)
                || !nbt.hasKey(NBT_DIMENSION, TAG_INT)
                || !nbt.hasKey(NBT_CONTROLLER_X, TAG_INT)
                || !nbt.hasKey(NBT_CONTROLLER_Y, TAG_INT)
                || !nbt.hasKey(NBT_CONTROLLER_Z, TAG_INT)
                || !nbt.hasKey(NBT_STATE, TAG_STRING)
                || !nbt.hasKey(NBT_ATTACHMENTS, TAG_LIST)) {
            return null;
        }

        UUID gateUuid = parseUuid(nbt.getString(NBT_GATE_UUID));
        RecordState state = RecordState.fromName(nbt.getString(NBT_STATE));
        if (gateUuid == null || state == null) {
            return null;
        }

        NBTTagList attachmentList = (NBTTagList)nbt.getTag(NBT_ATTACHMENTS);
        if (attachmentList.tagCount() <= 0
                || attachmentList.tagCount() > MAX_ATTACHMENTS_PER_GATE
                || attachmentList.func_150303_d() != TAG_COMPOUND) {
            return null;
        }

        List<Attachment> attachments =
                new ArrayList<Attachment>(attachmentList.tagCount());
        for (int i = 0; i < attachmentList.tagCount(); ++i) {
            NBTTagCompound attachmentNbt =
                    attachmentList.getCompoundTagAt(i);
            Attachment attachment = readAttachment(attachmentNbt);
            if (attachment == null) {
                return null;
            }
            attachments.add(attachment);
        }

        return new GateRecord(
                gateUuid,
                nbt.getInteger(NBT_DIMENSION),
                nbt.getInteger(NBT_CONTROLLER_X),
                nbt.getInteger(NBT_CONTROLLER_Y),
                nbt.getInteger(NBT_CONTROLLER_Z),
                state,
                attachments
        );
    }

    private Attachment readAttachment(NBTTagCompound nbt) {
        if (nbt == null
                || !nbt.hasKey(NBT_KIND, TAG_STRING)
                || !nbt.hasKey(NBT_RELATIVE_SUPPORT_X, TAG_INT)
                || !nbt.hasKey(NBT_RELATIVE_SUPPORT_Y, TAG_INT)
                || !nbt.hasKey(NBT_RELATIVE_SUPPORT_Z, TAG_INT)
                || !nbt.hasKey(NBT_LEAF, TAG_STRING)
                || !nbt.hasKey(NBT_ENTITY_UUID, TAG_STRING)
                || !nbt.hasKey(NBT_ENTITY, TAG_COMPOUND)) {
            return null;
        }

        AttachmentKind kind = AttachmentKind.fromName(nbt.getString(NBT_KIND));
        GateLeaf leaf = GateLeaf.fromSerializedName(nbt.getString(NBT_LEAF));
        UUID entityUuid = parseUuid(nbt.getString(NBT_ENTITY_UUID));
        if (kind == null
                || leaf == null
                || leaf.isSplitCenter()
                || entityUuid == null) {
            return null;
        }

        NBTTagCompound entityNbt = nbt.getCompoundTag(NBT_ENTITY);
        if (entityNbt == null || entityNbt.hasNoTags()) {
            return null;
        }

        return new Attachment(
                kind,
                nbt.getInteger(NBT_RELATIVE_SUPPORT_X),
                nbt.getInteger(NBT_RELATIVE_SUPPORT_Y),
                nbt.getInteger(NBT_RELATIVE_SUPPORT_Z),
                leaf,
                entityUuid,
                entityNbt,
                false
        );
    }

    private void rejectLoadedData(String reason) {
        readOnlyDueToInvalidData = true;
        FMLLog.severe(
                "[LOTRMoreMobs] Siege Gate banner attachment data is read-only: %s",
                reason == null ? "invalid persisted data" : reason
        );
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static final class PrepareResult {
        private final boolean successful;
        private final boolean attachmentsCaptured;
        private final String error;

        private PrepareResult(
                boolean successful,
                boolean attachmentsCaptured,
                String error
        ) {
            this.successful = successful;
            this.attachmentsCaptured = attachmentsCaptured;
            this.error = error;
        }

        static PrepareResult success(boolean attachmentsCaptured) {
            return new PrepareResult(true, attachmentsCaptured, null);
        }

        static PrepareResult failure(String error) {
            return new PrepareResult(false, false, error);
        }

        public boolean isSuccessful() {
            return successful;
        }

        public boolean hasCapturedAttachments() {
            return attachmentsCaptured;
        }

        public String getError() {
            return error;
        }
    }

    private enum RecordState {
        PREPARED,
        ATTACHED,
        RESTORING;

        static RecordState fromName(String name) {
            if (name == null) {
                return null;
            }
            try {
                return valueOf(name);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    private enum AttachmentKind {
        STANDING,
        WALL;

        static AttachmentKind fromName(String name) {
            if (name == null) {
                return null;
            }
            try {
                return valueOf(name);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    private enum RestoreResult {
        WAIT,
        RESTORED
    }

    private static final class GateRecord {
        private final UUID gateUuid;
        private final int dimension;
        private final int controllerX;
        private final int controllerY;
        private final int controllerZ;
        private RecordState state;
        private final List<Attachment> attachments;

        private GateRecord(
                UUID gateUuid,
                int dimension,
                int controllerX,
                int controllerY,
                int controllerZ,
                RecordState state,
                List<Attachment> attachments
        ) {
            this.gateUuid = gateUuid;
            this.dimension = dimension;
            this.controllerX = controllerX;
            this.controllerY = controllerY;
            this.controllerZ = controllerZ;
            this.state = state;
            this.attachments = attachments;
        }
    }

    private static final class Attachment {
        private final AttachmentKind kind;
        private final int relativeSupportX;
        private final int relativeSupportY;
        private final int relativeSupportZ;
        private final GateLeaf leaf;
        private final UUID entityUuid;
        private final NBTTagCompound entityNbt;
        private boolean restored;

        private Attachment(
                AttachmentKind kind,
                int relativeSupportX,
                int relativeSupportY,
                int relativeSupportZ,
                GateLeaf leaf,
                UUID entityUuid,
                NBTTagCompound entityNbt,
                boolean restored
        ) {
            this.kind = kind;
            this.relativeSupportX = relativeSupportX;
            this.relativeSupportY = relativeSupportY;
            this.relativeSupportZ = relativeSupportZ;
            this.leaf = leaf;
            this.entityUuid = entityUuid;
            this.entityNbt = (NBTTagCompound)entityNbt.copy();
            this.restored = restored;
        }
    }

    private static final class SupportPosition {
        private final int x;
        private final int y;
        private final int z;

        private SupportPosition(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof SupportPosition)) {
                return false;
            }
            SupportPosition position = (SupportPosition)other;
            return x == position.x && y == position.y && z == position.z;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }
    }
}
