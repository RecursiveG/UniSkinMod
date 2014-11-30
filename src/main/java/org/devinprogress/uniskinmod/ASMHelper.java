package org.devinprogress.uniskinmod;

import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Method;
import java.util.*;

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

public class ASMHelper {
    private Object obj;
    //Map<DeobfuscatedClassName,Map<methodName+Desc,processMethod>>
    private Map<String,Map<String,Method>> map;

    public ASMHelper(Object o){
        obj=o;
        map=new HashMap<String, Map<String,Method>>();
    }

    public void hookMethod(String className,String srgName,String mcpName,String desc,String targetTransformer){
        if(!map.containsKey(className))
            map.put(className,new HashMap<String, Method>());
        Method m=null;
        try{
            m= obj.getClass().getDeclaredMethod(targetTransformer,MethodNode.class);
        }catch(Exception e){
            e.printStackTrace();
        }
        map.get(className).put(srgName + desc, m);
        map.get(className).put(mcpName + desc, m);
    }

    public byte[] transform(String obfClassName,String className,byte[] bytes){
        if(!map.containsKey(className))return bytes;
        Map<String,Method> transMap=map.get(className);

        ClassReader cr=new ClassReader(bytes);
        ClassNode cn=new ClassNode();
        cr.accept(cn, 0);

        for(MethodNode mn:cn.methods){
            String methodName=FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(obfClassName,mn.name,mn.desc);
            String methodDesc=FMLDeobfuscatingRemapper.INSTANCE.mapMethodDesc(mn.desc);
            if(transMap.containsKey(methodName+methodDesc)){
                try{
                    transMap.get(methodName+methodDesc).invoke(obj,mn);
                }catch(Exception e){
                    e.printStackTrace();
                    return bytes;
                }
            }
        }

        ClassWriter cw=new ClassWriter(0);
        cn.accept(cw);
        return cw.toByteArray();
    }

    public static AbstractInsnNode getNthInsnNode(MethodNode mn,int opcode,int N){
        AbstractInsnNode n=mn.instructions.getFirst();
        int count=0;
        while(n!=null){
            if(n.getOpcode()==opcode){
                count++;
                if(count==N)
                    break;
            }
            n=n.getNext();
        }
        return n;
    }

    public static AbstractInsnNode getLabel(AbstractInsnNode n){
        if(!(n instanceof JumpInsnNode)){
            throw new RuntimeException("Not a valid JumpInsnNode");
        }else{
            return ((JumpInsnNode)n).label;
        }
    }
    public static void InsertInvokeStaticBefore(MethodNode mn,AbstractInsnNode n,String targetClass,String targetMethod,String desc){
        mn.instructions.insertBefore(n, new MethodInsnNode(Opcodes.INVOKESTATIC,
                targetClass.replace('.', '/'), targetMethod, desc,false));
    }
    public static AbstractInsnNode getNthALOAD(MethodNode mn,int index,int val){
        AbstractInsnNode n=mn.instructions.getFirst();
        int count=0;
        while(n!=null){
            if(n.getOpcode()==Opcodes.ALOAD&&((VarInsnNode)n).var==val){
                count++;
                if(count==index)
                    break;
            }
            n=n.getNext();
        }
        return n;
    }
}