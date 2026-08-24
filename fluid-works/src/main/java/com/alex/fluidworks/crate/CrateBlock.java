package com.alex.fluidworks.crate;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ItemScatterer;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jetbrains.annotations.Nullable;

/** Six-direction scalable crate; matching structures are bounded to a 9x9x9 box. */
public final class CrateBlock extends BlockWithEntity {
    public static final BooleanProperty NORTH=BooleanProperty.of("north"), EAST=BooleanProperty.of("east"),
        SOUTH=BooleanProperty.of("south"), WEST=BooleanProperty.of("west"), UP=BooleanProperty.of("up"),
        DOWN=BooleanProperty.of("down");
    private final String wood;
    private final MapCodec<CrateBlock> codec = MapCodec.unit(this);
    public CrateBlock(String wood, Settings settings) {
        super(settings); this.wood=wood;
        setDefaultState(getDefaultState().with(NORTH,false).with(EAST,false).with(SOUTH,false)
            .with(WEST,false).with(UP,false).with(DOWN,false));
    }
    public String wood() { return wood; }
    @Override protected MapCodec<? extends BlockWithEntity> getCodec(){return codec;}
    @Override protected void appendProperties(StateManager.Builder<Block,BlockState> b){b.add(NORTH,EAST,SOUTH,WEST,UP,DOWN);}
    @Override public @Nullable BlockState getPlacementState(ItemPlacementContext c){
        return exceeds(c.getWorld(),c.getBlockPos())?null:connections(getDefaultState(),c.getWorld(),c.getBlockPos());
    }
    @Override protected BlockState getStateForNeighborUpdate(BlockState s, WorldView w, ScheduledTickView t,
        BlockPos p, Direction d, BlockPos np, BlockState ns, Random r){return s.with(prop(d),ns.isOf(this));}
    private BlockState connections(BlockState s, net.minecraft.world.BlockView w, BlockPos p){
        for(Direction d:Direction.values())s=s.with(prop(d),w.getBlockState(p.offset(d)).isOf(this)); return s;
    }
    private boolean exceeds(net.minecraft.world.BlockView w,BlockPos origin){
        int minX=origin.getX(),maxX=minX,minY=origin.getY(),maxY=minY,minZ=origin.getZ(),maxZ=minZ;
        java.util.ArrayDeque<BlockPos> q=new java.util.ArrayDeque<>(); java.util.HashSet<BlockPos> seen=new java.util.HashSet<>();
        q.add(origin);seen.add(origin);
        while(!q.isEmpty()) { BlockPos p=q.removeFirst(); minX=Math.min(minX,p.getX());maxX=Math.max(maxX,p.getX());
            minY=Math.min(minY,p.getY());maxY=Math.max(maxY,p.getY());minZ=Math.min(minZ,p.getZ());maxZ=Math.max(maxZ,p.getZ());
            for(Direction d:Direction.values()){BlockPos n=p.offset(d);if(seen.add(n)&&w.getBlockState(n).isOf(this))q.add(n);} }
        return maxX-minX>=9||maxY-minY>=9||maxZ-minZ>=9;
    }
    private static BooleanProperty prop(Direction d){return switch(d){case NORTH->NORTH;case EAST->EAST;case SOUTH->SOUTH;case WEST->WEST;case UP->UP;case DOWN->DOWN;};}
    @Override protected BlockRenderType getRenderType(BlockState s){return BlockRenderType.MODEL;}
    @Override public @Nullable BlockEntity createBlockEntity(BlockPos p,BlockState s){return new CrateBlockEntity(p,s);}
    @Override protected ActionResult onUse(BlockState s,World w,BlockPos p,PlayerEntity player,BlockHitResult hit){
        if(!w.isClient()){CrateClusterInventory inv=CrateClusterInventory.create(w,p,this);
            if (w.getBlockEntity(p) instanceof CrateBlockEntity clicked) {
                inv = CrateClusterInventory.create(w, p, this, clicked.selectedPage());
                if (player.isSneaking()) {
                    int selected = clicked.advancePage(inv.pageCount());
                    player.sendMessage(Text.translatable("container.fluidworks.crate_page_selected",
                        selected + 1, inv.pageCount()), true);
                    return ActionResult.SUCCESS;
                }
            }
            Text title=Text.translatable("container.fluidworks.scaled_crate",Text.translatable(getTranslationKey()),
                inv.pageNumber(),inv.pageCount(),inv.totalSlots());
            CrateClusterInventory selectedInventory = inv;
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory((id,pi,pl)->
                GenericContainerScreenHandler.createGeneric9x6(id,pi,selectedInventory),title));}
        return ActionResult.SUCCESS;
    }
    @Override protected void onStateReplaced(BlockState s,ServerWorld w,BlockPos p,boolean moved){
        if(!moved&&w.getBlockEntity(p)instanceof CrateBlockEntity c)ItemScatterer.spawn(w,p,c);
        super.onStateReplaced(s,w,p,moved);
    }
}
