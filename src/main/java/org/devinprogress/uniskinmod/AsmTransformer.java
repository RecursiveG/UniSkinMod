package org.devinprogress.uniskinmod;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class AsmTransformer extends BaseAsmTransformer {
    private static final String INVOKE_TARGET_CLASS=UniSkinMod.class.getName().replace(".","/");

    public AsmTransformer(){
        hookMethod("net.minecraft.client.network.NetworkPlayerInfo","<init>","<init>",
                "(Lnet/minecraft/network/play/server/S38PacketPlayerListItem$AddPlayerData;)V",
                new NetworkPlayerInfoTransformer());
        hookMethod("net.minecraft.client.network.NetworkPlayerInfo","<init>","<init>",
                "(Lcom/mojang/authlib/GameProfile;)V",
                new NetworkPlayerInfoTransformer());
        hookMethod("net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer","func_180543_a","renderSkull",
                "(FFFLnet/minecraft/util/EnumFacing;FILcom/mojang/authlib/GameProfile;I)V",
                new SkullHijackTransformer());
        hookMethod("net.minecraft.client.renderer.ImageBufferDownload","func_78433_b",
                "setAreaOpaque","(IIII)V",new setAreaOpaqueTransformer());
        hookMethod("com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService","getTextures","getTextures",
                "(Lcom/mojang/authlib/GameProfile;Z)Ljava/util/Map;",
                new getTexturesTransformer());
    }

    private class NetworkPlayerInfoTransformer implements  IMethodTransformer{
        @Override
        public void transform(MethodNode mn, String srgName, boolean devEnv, String classObfName) {
            final String FIELD_NAME=devEnv?"gameProfile":"a"; //field_178867_a
            AbstractInsnNode n= getInsnPutField(mn, FIELD_NAME, 1);
            mn.instructions.insertBefore(n, new MethodInsnNode(Opcodes.INVOKESTATIC,INVOKE_TARGET_CLASS,
                    "fillGameProfile","(Lcom/mojang/authlib/GameProfile;)Lcom/mojang/authlib/GameProfile;",false));
        }
    }

    private class SkullHijackTransformer implements IMethodTransformer{
        @Override
        public void transform(MethodNode mn, String srgName, boolean devEnv, String classObfName) {
            AbstractInsnNode n=mn.instructions.getFirst();
            mn.instructions.insertBefore(n,new VarInsnNode(Opcodes.ALOAD,7));
            mn.instructions.insertBefore(n, new MethodInsnNode(Opcodes.INVOKESTATIC,INVOKE_TARGET_CLASS,
                    "fillGameProfile","(Lcom/mojang/authlib/GameProfile;)Lcom/mojang/authlib/GameProfile;",false));
            mn.instructions.insertBefore(n,new VarInsnNode(Opcodes.ASTORE,7));
        }
    }

    private class setAreaOpaqueTransformer implements IMethodTransformer{
        @Override
        public void transform(MethodNode mn, String srgName, boolean devEnv, String classObfName) {
            mn.instructions.clear();
            mn.instructions.add(new InsnNode(Opcodes.RETURN));
            mn.maxLocals=1;mn.maxStack=0;
        }
    }

    /** Bypass the Signature Verification */
    private class getTexturesTransformer implements IMethodTransformer{
        @Override
        public void transform(MethodNode mn, String srgName, boolean devEnv, String classObfName) {
            AbstractInsnNode n=getNthInsnNode(mn,Opcodes.ALOAD,1);
            mn.instructions.insertBefore(n,new InsnNode(Opcodes.ICONST_0));
            mn.instructions.insertBefore(n,new VarInsnNode(Opcodes.ISTORE,2));
        }
    }
}
