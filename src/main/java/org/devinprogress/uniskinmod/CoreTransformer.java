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
        asm.add("net.minecraft.client.renderer.ThreadDownloadImageData$1","run","run","()V","()V","hijackURL");
    }

    public static void hijackURL(MethodNode mn){
        AbstractInsnNode n=ASMHelper.getNthInsnNode(mn,Opcodes.INVOKESTATIC,1);
        ASMHelper.InsertInvokeStaticAfter(mn,n,"org.devinprogress.uniskinmod.SkinCore","alterURL","(Ljava/lang/String;)Ljava/lang/String;");
    }

	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
        return asm.transform(transformedName,bytes);
	}
}
