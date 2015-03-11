package org.devinprogress.uniskinmod;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/**
 * Transformers for authlib-1.5.17 and MC1.8
 */
public class AsmTransformer extends BaseAsmTransformer {
    private static final String INVOKE_TARGET_CLASS=UniSkinMod.class.getName().replace(".","/");

    public AsmTransformer(){
        hookMethod("com.mojang.authlib.yggdrasil.YggdrasilGameProfileRepository","findProfilesByNames","findProfilesByNames",
                "([Ljava/lang/String;Lcom/mojang/authlib/Agent;Lcom/mojang/authlib/ProfileLookupCallback;)V",
                new findProfilesByNamesTransformer());
        hookMethod("com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService","fillGameProfile","fillGameProfile",
                "(Lcom/mojang/authlib/GameProfile;Z)Lcom/mojang/authlib/GameProfile;",
                new fillGameProfileTransformer());
        hookMethod("net.minecraft.client.renderer.ImageBufferDownload","func_78433_b",
                "setAreaOpaque","(IIII)V",new setAreaOpaqueTransformer());
        hookMethod("com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService","getTextures","getTextures",
                "(Lcom/mojang/authlib/GameProfile;Z)Ljava/util/Map;",
                new getTexturesTransformer());
    }

    private class findProfilesByNamesTransformer implements IMethodTransformer{
        @Override
        public void transform(MethodNode mn, String srgName, boolean devEnv, String classObfName) {
            AbstractInsnNode n=getNthInsnNode(mn, Opcodes.CHECKCAST,2).getNext();
            mn.instructions.insertBefore(n,new VarInsnNode(Opcodes.ALOAD,7));
            mn.instructions.insertBefore(n,new MethodInsnNode(Opcodes.INVOKESTATIC,INVOKE_TARGET_CLASS,
                    "fillMissionProfile","(Lcom/mojang/authlib/yggdrasil/response/ProfileSearchResultsResponse;Ljava/util/List;)Lcom/mojang/authlib/yggdrasil/response/ProfileSearchResultsResponse;",false));
        }
    }

    private class fillGameProfileTransformer implements IMethodTransformer{
        @Override
        public void transform(MethodNode mn, String srgName, boolean devEnv, String classObfName) {
            AbstractInsnNode n=getNthInsnNode(mn,Opcodes.ALOAD,3);
            LabelNode label=new LabelNode();
            LabelNode label2=new LabelNode();
            mn.instructions.insertBefore(n,new VarInsnNode(Opcodes.ALOAD,1));
            mn.instructions.insertBefore(n,new MethodInsnNode(Opcodes.INVOKESTATIC,INVOKE_TARGET_CLASS,
                    "isOnlinePlayer","(Lcom/mojang/authlib/GameProfile;)Z",false));
            mn.instructions.insertBefore(n,new JumpInsnNode(Opcodes.IFEQ,label));

            n=getNthInsnNode(mn,Opcodes.ASTORE,3);
            mn.instructions.insertBefore(n,new JumpInsnNode(Opcodes.GOTO,label2));
            mn.instructions.insertBefore(n,label);
            mn.instructions.insertBefore(n,new InsnNode(Opcodes.ACONST_NULL));
            mn.instructions.insertBefore(n,label2);

            n=getNthInsnNode(mn,Opcodes.ALOAD,6);
            mn.instructions.insertBefore(n,new VarInsnNode(Opcodes.ALOAD,1));
            mn.instructions.insertBefore(n,new VarInsnNode(Opcodes.ALOAD,4));
            mn.instructions.insertBefore(n,new MethodInsnNode(Opcodes.INVOKESTATIC,INVOKE_TARGET_CLASS,
                    "fillResponse","(Lcom/mojang/authlib/GameProfile;Lcom/mojang/authlib/yggdrasil/response/MinecraftProfilePropertiesResponse;)Lcom/mojang/authlib/yggdrasil/response/MinecraftProfilePropertiesResponse;",false));
            mn.instructions.insertBefore(n,new VarInsnNode(Opcodes.ASTORE,4));
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

    private class getTexturesTransformer implements IMethodTransformer{
        @Override
        public void transform(MethodNode mn, String srgName, boolean devEnv, String classObfName) {
            AbstractInsnNode n=getNthInsnNode(mn,Opcodes.ALOAD,1);
            mn.instructions.insertBefore(n,new InsnNode(Opcodes.ICONST_0));
            mn.instructions.insertBefore(n,new VarInsnNode(Opcodes.ISTORE,2));
        }
    }
}
