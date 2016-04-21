package org.devinprogress.uniskinmod.coremod;

import org.devinprogress.uniskinmod.UniSkinMod;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Collections;
import java.util.Map;

public class AsmTransformer extends BaseAsmTransformer {
    private static final String INVOKE_TARGET_CLASS = UniSkinMod.class.getName().replace(".", "/");

    public AsmTransformer() {
        super();
    }

    /**
     * Invalid the ImageBufferDownload.setAreaOpaque() method, allowing transparent skin textures
     * Make the method returns immediately.
     */
    @RegisterTransformer(
            className = "net.minecraft.client.renderer.ImageBufferDownload",
            srgName = "func_78433_b",
            mcpName = "setAreaOpaque",
            desc = "(IIII)V"
    )
    public static class setAreaOpaqueTransformer implements IMethodTransformer {
        @Override
        public void transform(ClassNode cn, String classObfName, MethodNode mn, String srgName, boolean devEnv) {
            mn.instructions.clear();
            mn.instructions.add(new InsnNode(Opcodes.RETURN));
            mn.maxLocals = 1;
            mn.maxStack = 0;
        }
    }

    @RegisterTransformer(
            className = "net.minecraft.client.renderer.ImageBufferDownload",
            srgName = "func_78434_a",
            mcpName = "setAreaTransparent",
            desc = "(IIII)V"
    )
    public static class setAreaTransparentTransformer extends setAreaOpaqueTransformer{}

    /**
     * Hijack YggdrasilMinecraftSessionService.isWhitelistedDomain(String url)
     * Force it to return True.
     * <p/>
     * Codes to be inserted, at the beginning:
     * <pre>
     *     ICONST_1
     *     IRETURN
     * </pre>
     */
    @RegisterTransformer(
            className = "com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService",
            mcpName = "isWhitelistedDomain",
            desc = "(Ljava/lang/String;)Z"
    )
    public static class isWhitelistedDomainTransformer implements IMethodTransformer {
        @Override
        public void transform(ClassNode cn, String classObfName, MethodNode mn, String srgName, boolean devEnv) {
            AbstractInsnNode n = mn.instructions.get(0);
            mn.instructions.insertBefore(n, new InsnNode(Opcodes.ICONST_1));
            mn.instructions.insertBefore(n, new InsnNode(Opcodes.IRETURN));
        }
    }

    /**
     * Wrap UniSkinMod.getTextures_wrapper() around YggdrasilMinecraftSessionService.getTextures
     * Rename it to getTextures_old() and implements IGetTexture_old
     */
    @RegisterTransformer(
            className = "com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService",
            mcpName = "getTextures",
            desc = "(Lcom/mojang/authlib/GameProfile;Z)Ljava/util/Map;"
    )
    public static class getTextureWrapper implements IMethodTransformer {
        @Override
        public void transform(ClassNode cn, String classObfName, MethodNode mn, String srgName, boolean devEnv) {
            MethodNode wrapperMethod = new MethodNode(mn.access, mn.name, mn.desc, null,
                    mn.exceptions.toArray(new String[mn.exceptions.size()]));
            mn.name = "getTextures_old";
            mn.access = Opcodes.ACC_PUBLIC;

            wrapperMethod.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            wrapperMethod.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
            wrapperMethod.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            wrapperMethod.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, INVOKE_TARGET_CLASS, "getTextures_wrapper",
                    "(Lcom/mojang/authlib/GameProfile;ZLorg/devinprogress/uniskinmod/coremod/IGetTexture_old;)Ljava/util/Map;", false));
            wrapperMethod.instructions.add(new InsnNode(Opcodes.ARETURN));
            wrapperMethod.maxStack = 3;

            cn.methods.add(wrapperMethod);
            cn.interfaces.add(IGetTexture_old.class.getName().replace(".", "/"));
        }
    }

    /**
     * Append following codes to SkinManager$3$1.
     * Otherwise static elytra textures will not be loaded.
     * <pre>
     *     if (map.containsKey(Type.ELYTRA))
     *     {
     *         SkinManager.this.loadSkin((MinecraftProfileTexture)map.get(Type.ELYTRA), Type.ELYTRA, skinAvailableCallback);
     *     }
     * </pre>
     */

    @RegisterTransformer(
            className = "net.minecraft.client.resources.SkinManager$3$1",
            mcpName = "run",
            desc = "()V"
    )
    public static class skinManagerLoadElytraTransformer implements IMethodTransformer {
        @Override
        public void transform(ClassNode cn, String classObfName, MethodNode mn, String srgName, boolean devEnv) {
            // printMethod(mn, null, null);

            AbstractInsnNode start = getNthInsnNode(mn, Opcodes.ALOAD, 1); // copied
            AbstractInsnNode end = ((JumpInsnNode) getNthInsnNode(mn, Opcodes.IFEQ, 1)).label; // not copied
            AbstractInsnNode target = ((JumpInsnNode) getNthInsnNode(mn, Opcodes.IFEQ, 2)).label.getNext();

            /* duplicate existed codes, replace SKIN with ELYTRA, also replace labels */
            LabelNode labelNode = new LabelNode();
            Map<LabelNode, LabelNode> labelMap = Collections.singletonMap((LabelNode) end, labelNode);
            for (AbstractInsnNode i = start; i != end; i = i.getNext()) {
                if (i instanceof LabelNode || i instanceof LineNumberNode) continue;
                AbstractInsnNode copy = i.clone(labelMap);
                if (copy.getOpcode() == Opcodes.GETSTATIC) ((FieldInsnNode) copy).name = "ELYTRA";
                mn.instructions.insertBefore(target, copy);
            }
            mn.instructions.insertBefore(target, labelNode);
        }
    }

    /**
     * Transformer for AbstractClientPlayer.getLocationXXX() functions
     */
    public static abstract class BaseGetDynamicTransformer implements IMethodTransformer {
        public void transform(MethodNode mn, String target, String type) {
            AbstractInsnNode n = getNthInsnNode(mn, Opcodes.ASTORE, 1).getNext();
            LabelNode label1 = new LabelNode();

            mn.instructions.insertBefore(n, new VarInsnNode(Opcodes.ALOAD, 1));
            mn.instructions.insertBefore(n, new MethodInsnNode(Opcodes.INVOKESTATIC, INVOKE_TARGET_CLASS, target, "(Lnet/minecraft/client/network/NetworkPlayerInfo;)" + type, false));
            mn.instructions.insertBefore(n, new JumpInsnNode(Opcodes.IFNULL, label1));
            mn.instructions.insertBefore(n, new VarInsnNode(Opcodes.ALOAD, 1));
            mn.instructions.insertBefore(n, new MethodInsnNode(Opcodes.INVOKESTATIC, INVOKE_TARGET_CLASS, target, "(Lnet/minecraft/client/network/NetworkPlayerInfo;)" + type, false));
            mn.instructions.insertBefore(n, new InsnNode(Opcodes.ARETURN));
            mn.instructions.insertBefore(n, label1);
            mn.maxStack = 2;
        }
    }

    @RegisterTransformer(
            className = "net.minecraft.client.entity.AbstractClientPlayer",
            srgName = "func_110306_p",
            mcpName = "getLocationSkin",
            desc = "()Lnet/minecraft/util/ResourceLocation;"

    )
    public static class getDynamicSkinTransformer extends BaseGetDynamicTransformer {
        @Override
        public void transform(ClassNode cn, String classObfName, MethodNode mn, String srgName, boolean devEnv) {
            transform(mn, "getDynamicSkinResource", "Lnet/minecraft/util/ResourceLocation;");
        }
    }

    @RegisterTransformer(
            className = "net.minecraft.client.entity.AbstractClientPlayer",
            srgName = "func_110303_q",
            mcpName = "getLocationCape",
            desc = "()Lnet/minecraft/util/ResourceLocation;"

    )
    public static class getDynamicCapeTransformer extends BaseGetDynamicTransformer {
        @Override
        public void transform(ClassNode cn, String classObfName, MethodNode mn, String srgName, boolean devEnv) {
            transform(mn, "getDynamicCapeResource", "Lnet/minecraft/util/ResourceLocation;");
        }
    }

    @RegisterTransformer(
            className = "net.minecraft.client.entity.AbstractClientPlayer",
            srgName = "func_184834_t",
            mcpName = "getLocationElytra",
            desc = "()Lnet/minecraft/util/ResourceLocation;"

    )
    public static class getDynamicElytraTransformer extends BaseGetDynamicTransformer {
        @Override
        public void transform(ClassNode cn, String classObfName, MethodNode mn, String srgName, boolean devEnv) {
            transform(mn, "getDynamicElytraResource", "Lnet/minecraft/util/ResourceLocation;");
        }
    }

    @RegisterTransformer(
            className = "net.minecraft.client.entity.AbstractClientPlayer",
            srgName = "func_175154_l",
            mcpName = "getSkinType",
            desc = "()Ljava/lang/String;"

    )
    public static class getDynamicSkinModelTransformer extends BaseGetDynamicTransformer {
        @Override
        public void transform(ClassNode cn, String classObfName, MethodNode mn, String srgName, boolean devEnv) {
            transform(mn, "getDynamicSkinModel", "Ljava/lang/String;");
        }
    }

    /**
     * Insert codes before bindTexture(). Making skulls also render dynamic skin.
     * <pre>
     *     resourcelocaton = UniSkinMod.getDynamicSkinResourceForSkull(p_188190_7_, resourcelocaton)
     * </pre>
     * LocalVars : resourcelocaton @11
     * p_188190_7_      @7
     */
    @RegisterTransformer(
            className = "net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer",
            srgName = "func_188190_a",
            mcpName = "renderSkull",
            desc = "(FFFLnet/minecraft/util/EnumFacing;FILcom/mojang/authlib/GameProfile;IF)V"
    )
    public static class dynamicSkullTransformer implements IMethodTransformer {
        @Override
        public void transform(ClassNode cn, String classObfName, MethodNode mn, String srgName, boolean devEnv) {
            AbstractInsnNode n = getNthInsnNode(mn, Opcodes.IFNULL, 1);
            JumpInsnNode jn = (JumpInsnNode) n;
            n = jn.label.getNext();

            mn.instructions.insertBefore(n, new VarInsnNode(Opcodes.ALOAD, 7));
            mn.instructions.insertBefore(n, new VarInsnNode(Opcodes.ALOAD, 11));
            mn.instructions.insertBefore(n, new MethodInsnNode(Opcodes.INVOKESTATIC, INVOKE_TARGET_CLASS, "getDynamicSkinResourceForSkull",
                    "(Lcom/mojang/authlib/GameProfile;Lnet/minecraft/util/ResourceLocation;)Lnet/minecraft/util/ResourceLocation;", false));
            mn.instructions.insertBefore(n, new VarInsnNode(Opcodes.ASTORE, 11));
        }
    }

    /*
     * Make SkinManager.cache load textures asynchronously
    @RegisterTransformer(
            className = "net.minecraft.client.resources.SkinManager",
            mcpName = "<init>",
            desc = "(Lnet/minecraft/client/renderer/texture/TextureManager;Ljava/io/File;Lcom/mojang/authlib/minecraft/MinecraftSessionService;)V"

    )
    public static class delayedLoadingCacheTransformaer implements IMethodTransformer {
        @Override
        public void transform(ClassNode cn, String classObfName, MethodNode mn, String srgName, boolean devEnv) {
            AbstractInsnNode n = getNthInsnNode(mn, Opcodes.PUTFIELD, 4);
            mn.instructions.insertBefore(n, new MethodInsnNode(Opcodes.INVOKESTATIC, "org/devinprogress/uniskinmod/coremod/DelayedLoadingCache",
                    "wrap", "(Lcom/google/common/cache/LoadingCache;)Lcom/google/common/cache/LoadingCache;", false));
        }
    }
    */

    /**
     * Make SkinManager.loadSkinFromCache(), which is used by skull
     * texture loading, loads textures asynchronously.
     * Thus avoids main thread freezing.
     */
    @RegisterTransformer(
            className = "net.minecraft.client.resources.SkinManager",
            mcpName = "loadSkinFromCache",
            srgName = "func_152788_a",
            desc = "(Lcom/mojang/authlib/GameProfile;)Ljava/util/Map;"
    )
    public static class asyncLoadingCacheTransformer implements IMethodTransformer {
        @Override
        public void transform(ClassNode cn, String classObfName, MethodNode mn, String srgName, boolean devEnv) {
            MethodNode wrapperMethod = new MethodNode(mn.access, mn.name, mn.desc, null,
                    mn.exceptions.toArray(new String[mn.exceptions.size()]));
            mn.name = "loadSkinFromCache_old";
            mn.access = Opcodes.ACC_PUBLIC;

            wrapperMethod.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            wrapperMethod.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            wrapperMethod.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, INVOKE_TARGET_CLASS, "loadSkinFromCache_wrapper",
                    "(Lcom/mojang/authlib/GameProfile;Lorg/devinprogress/uniskinmod/coremod/ILoadSkinFromCache_old;)Ljava/util/Map;", false));
            wrapperMethod.instructions.add(new InsnNode(Opcodes.ARETURN));
            wrapperMethod.maxStack = 3;

            cn.methods.add(wrapperMethod);
            cn.interfaces.add(ILoadSkinFromCache_old.class.getName().replace(".", "/"));

            MethodNode getCacheMethod = new MethodNode(Opcodes.ACC_PUBLIC, "getLoadingCache", "()Lcom/google/common/cache/LoadingCache;", null, new String[0]);
            getCacheMethod.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            getCacheMethod.instructions.add(getNthInsnNode(mn, Opcodes.GETFIELD, 1).clone(Collections.<LabelNode, LabelNode>emptyMap()));
            getCacheMethod.instructions.add(new InsnNode(Opcodes.ARETURN));
            cn.methods.add(getCacheMethod);
        }
    }
}