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

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

public class CoreTransformer implements IClassTransformer {
    private ASMHelper asm=null;

    public CoreTransformer(){
        asm=new ASMHelper(this);
        asm.add("net.minecraft.client.entity.AbstractClientPlayer","c","getSkinUrl","(Ljava/lang/String;)Ljava/lang/String;","(Ljava/lang/String;)Ljava/lang/String;","hijackSkinURL");
        asm.add("net.minecraft.client.entity.AbstractClientPlayer","d","getCapeUrl","(Ljava/lang/String;)Ljava/lang/String;","(Ljava/lang/String;)Ljava/lang/String;","hijackCloakURL");
    }

    public static void hijackSkinURL(MethodNode mn){
        mn.instructions.clear();
        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"org/devinprogress/uniskinmod/SkinCore","getSkinUrl","(Ljava/lang/String;)Ljava/lang/String;"));
        mn.instructions.add(new InsnNode(Opcodes.ARETURN));
        mn.maxStack=1;
        mn.maxLocals=1;
    }

    public static void hijackCloakURL(MethodNode mn){
        mn.instructions.clear();
        mn.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        mn.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"org/devinprogress/uniskinmod/SkinCore","getCloakUrl","(Ljava/lang/String;)Ljava/lang/String;"));
        mn.instructions.add(new InsnNode(Opcodes.ARETURN));
        mn.maxStack=1;
        mn.maxLocals=1;
    }

	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
        return asm.transform(transformedName,bytes);
	}
}
