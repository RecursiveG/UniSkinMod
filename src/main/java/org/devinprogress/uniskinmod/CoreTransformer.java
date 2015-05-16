package org.devinprogress.uniskinmod;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 This file is part of Universal Skin Mod,
 Copyright (C) 2014  RecursiveG

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along
 with this program; if not, write to the Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

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
        asm.hookMethod("net.minecraft.client.resources.SkinManager$3",        "run",         "run",          "()V",                 "injectCode");
        asm.hookMethod("net.minecraft.client.renderer.ImageBufferDownload",   "func_78433_b","setAreaOpaque","(IIII)V",             "noOpaque");
        //asm.hookMethod("com.mojang.authlib.minecraft.MinecraftProfileTexture","getHash",     "getHash",      "()Ljava/lang/String;","newHash");
    }

    public static void injectCode(MethodNode mn){
        // Code to be inserted:
        // org.devinprogress.uniskinmod.SkinCore.injectTexture(hashmap,p_152790_1_);
        // load hashmap using ALOAD_1
        AbstractInsnNode n=ASMHelper.getNthInsnNode(mn,Opcodes.GETFIELD,2);
        FieldInsnNode loadGameProfileToStack=(FieldInsnNode)(n.clone(null));
        n=ASMHelper.getNthInsnNode(mn, Opcodes.IFEQ,1);
        n=ASMHelper.getLabel(n).getNext();

        mn.instructions.insertBefore(n, new VarInsnNode(Opcodes.ALOAD,1));
        mn.instructions.insertBefore(n, new VarInsnNode(Opcodes.ALOAD,0));
        mn.instructions.insertBefore(n, loadGameProfileToStack);
        ASMHelper.InsertInvokeStaticBefore(mn,n,"org.devinprogress.uniskinmod.SkinCore","injectTexture","(Ljava/util/HashMap;Lcom/mojang/authlib/GameProfile;)V");
    }

    public static void noOpaque(MethodNode mn){
        mn.instructions.clear();
        mn.instructions.add(new InsnNode(Opcodes.RETURN));
        mn.maxLocals=1;mn.maxStack=0;
    }

    public static void newHash(MethodNode mn){
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
        return asm.transform(name,transformedName,bytes);
	}
}
