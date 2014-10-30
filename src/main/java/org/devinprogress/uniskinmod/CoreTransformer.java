package org.devinprogress.uniskinmod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class CoreTransformer implements IClassTransformer {
    // Hack into SkinManager$3.run()V
    // Insert code after the IF and hijack hashmap
    // Add MinecraftTextureProfile Manually
    // you have also pass p_152790_1_(GameProfile)
    
    // Hack into MinecraftProfileTexture.getHash()Ljava/lang/String;
    // SHA-1 of the url as the hash instead of the filename
    // workaround for different format of skin urls.
    private ASMHelper asm=null;

    public CoreTransformer(){
        asm=new ASMHelper(this);
        asm.add("net.minecraft.client.resources.SkinManager$3","run","run","()V","()V","injectCode");
        asm.add("net.minecraft.client.renderer.ImageBufferDownload","b","setAreaOpaque","(IIII)V","(IIII)V","noOpaque");
        asm.add("com.mojang.authlib.minecraft.MinecraftProfileTexture", "getHash", "getHash", "()Ljava/lang/String;", "()Ljava/lang/String;","newHash");
    }

    public static void injectCode(MethodNode mn){
        // Code to be inserted:
        // org.devinprogress.uniskinmod.SkinCore.injectTexture(hashmap,p_152790_1_);
        // load hashmap using ALOAD_1
        SkinCore.log("ASMTransformer Invoked");
        AbstractInsnNode n=ASMHelper.getNthInsnNode(mn,Opcodes.GETFIELD,2);
        FieldInsnNode loadGameProfileToStack=(FieldInsnNode) ((FieldInsnNode)n).clone(null);
        n=ASMHelper.getNthInsnNode(mn, Opcodes.IFEQ,1);
        n=ASMHelper.getLabel(n).getNext();
        
        mn.instructions.insertBefore(n, new VarInsnNode(Opcodes.ALOAD,1));
        mn.instructions.insertBefore(n, new VarInsnNode(Opcodes.ALOAD,0));
        mn.instructions.insertBefore(n, loadGameProfileToStack);
        ASMHelper.InsertInvokeStaticBefore(mn,n,"org.devinprogress.uniskinmod.SkinCore","injectTexture","(Ljava/util/HashMap;Lcom/mojang/authlib/GameProfile;)V");
    }

    public static void noOpaque(MethodNode mn){
        SkinCore.log("Opaque Transformer Hit");
        mn.instructions.clear();
        mn.instructions.add(new InsnNode(Opcodes.RETURN));
        mn.maxLocals=1;mn.maxStack=0;
    }
    
    public static void newHash(MethodNode mn){
        SkinCore.log("hash Transformer Hit");
        mn.instructions.clear();
        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));
        mn.instructions.add(new FieldInsnNode(Opcodes.GETFIELD,"com/mojang/authlib/minecraft/MinecraftProfileTexture","url","Ljava/lang/String;"));
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"org/devinprogress/uniskinmod/SkinCore","SHA1SUM","(Ljava/lang/String;)Ljava/lang/String;",false));
        mn.instructions.add(new InsnNode(Opcodes.ARETURN));
        mn.maxLocals=1;
        mn.maxStack=1;
    }
    
	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
        return asm.transform(transformedName,bytes);
	}
}
