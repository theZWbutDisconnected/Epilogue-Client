package epilogue.mixin;

import de.florianmichael.vialoadingbase.netty.event.CompressionReorderEvent;
import epilogue.Epilogue;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.GenericFutureListener;
import epilogue.event.EventManager;
import epilogue.event.types.EventType;
import epilogue.events.PacketEvent;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Future;

@SideOnly(Side.CLIENT)
@Mixin({NetworkManager.class})

public abstract class MixinNetworkManager {

    @Shadow
    private Channel channel;

    @Inject(
            method = {"channelRead0*"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void channelRead0(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo callbackInfo) {
        if (!packet.getClass().getName().startsWith("net.minecraft.network.play.client")) {
            if (Epilogue.delayManager != null && Epilogue.delayManager.shouldDelay((Packet<INetHandlerPlayClient>) packet)) {
                callbackInfo.cancel();
            } else {
                PacketEvent event = new PacketEvent(EventType.RECEIVE, packet);
                EventManager.call(event);
                if (event.isCancelled()) {
                    callbackInfo.cancel();
                }
            }
        }
    }

    @Inject(method = "setCompressionTreshold", at = @At("RETURN"))
    private void viaforge$reorderPipeline(int treshold, CallbackInfo ci) {
        channel.pipeline().fireUserEventTriggered(new CompressionReorderEvent());
    }

    @Inject(
            method = {"sendPacket(Lnet/minecraft/network/Packet;)V"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void sendPacket(Packet<?> packet, CallbackInfo callbackInfo) {
        if (!packet.getClass().getName().startsWith("net.minecraft.network.play.server")) {
            if (epilogue.viaforge.ViaForge.handle(packet)) {
                callbackInfo.cancel();
                return;
            }
            
            PacketEvent event = new PacketEvent(EventType.SEND, packet);
            EventManager.call(event);
            if (event.isCancelled()) {
                callbackInfo.cancel();
            } else if (Epilogue.playerStateManager != null && Epilogue.blinkManager != null && Epilogue.lagManager != null) {
                if (!Epilogue.lagManager.isFlushing()) {
                    Epilogue.playerStateManager.handlePacket(packet);
                    if (Epilogue.blinkManager.isBlinking()) {
                        if (Epilogue.blinkManager.offerPacket(packet)) {
                            callbackInfo.cancel();
                            return;
                        }
                    }
                    if (Epilogue.lagManager.handlePacket(packet)) {
                        callbackInfo.cancel();
                    }
                }
            }
        }
    }

    @Inject(
            method = {"sendPacket(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;[Lio/netty/util/concurrent/GenericFutureListener;)V"},
            at = {@At("HEAD")},
            cancellable = true
    )
    private void sendPacket2(
            Packet<?> packet,
            GenericFutureListener<? extends Future<? super Void>> genericFutureListener,
            GenericFutureListener<? extends Future<? super Void>>[] arr,
            CallbackInfo callbackInfo
    ) {
        if (!packet.getClass().getName().startsWith("net.minecraft.network.play.server")) {
            if (Epilogue.playerStateManager != null && Epilogue.blinkManager != null && Epilogue.lagManager != null) {
                if (!Epilogue.lagManager.isFlushing()) {
                    Epilogue.playerStateManager.handlePacket(packet);
                    if (Epilogue.blinkManager.isBlinking()) {
                        if (Epilogue.blinkManager.offerPacket(packet)) {
                            callbackInfo.cancel();
                            return;
                        }
                    }
                    if (Epilogue.lagManager.handlePacket(packet)) {
                        callbackInfo.cancel();
                    }
                }
            }
        }
    }
}
