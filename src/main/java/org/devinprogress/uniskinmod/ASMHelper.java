package org.devinprogress.uniskinmod;


import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by recursiveg on 14-10-21.
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
            //System.out.println(String.format("Examing Method: %s%s",mn.name,mn.desc));
            String methodName=FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(obfClassName,mn.name,mn.desc);
            String methodDesc=FMLDeobfuscatingRemapper.INSTANCE.mapMethodDesc(mn.desc);
            if(transMap.containsKey(methodName+methodDesc)){
                try{
                    //System.out.println(String.format("Invoking Method: %s%s",mn.name,mn.desc));
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

    public class MethodRecord{
        public String ClassName;
        public String MethodName;
        public String MethodNameDeobf;
        public String Desc;
        public String DescDeobf;
        public String ProcessMethodName;
        public Method ProcessMethod;
        private boolean flag=false;
        public MethodRecord(String a,String b,String c,String d,String e,String f){
            ClassName=a;
            MethodName=b;
            MethodNameDeobf=c;
            Desc=d;
            DescDeobf=e;
            ProcessMethodName=f;
        }

        public void preProcess(boolean Deobf,Object o){
            if(flag)return;

            if(Deobf){
                MethodName=MethodNameDeobf;
                Desc=DescDeobf;
            }
            try{
                ProcessMethod=o.getClass().getDeclaredMethod(ProcessMethodName,MethodNode.class);
            }catch(Exception e){
                e.printStackTrace();
            }
            flag=true;
        }
    }
}