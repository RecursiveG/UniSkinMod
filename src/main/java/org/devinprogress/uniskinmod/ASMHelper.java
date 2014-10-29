package org.devinprogress.uniskinmod;


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
    private List<MethodRecord> map=null;
    private Set<String> classMap=null;

    public ASMHelper(Object o){
        obj=o;
        map=new ArrayList<MethodRecord>();
        classMap=new HashSet<String>();
    }

    public void add(String className,String methodName,String methodNameDeobf,String Descripton,String DescriptionDeobf,String targetTransformer){
        map.add(new MethodRecord(
                className,
                methodName,
                methodNameDeobf,
                Descripton,
                DescriptionDeobf,
                targetTransformer
        ));
        classMap.add(className);
    }

    public byte[] transform(String className,byte[] bytes){

        if(!classMap.contains(className))return bytes;
        //System.out.println("Examing Class:"+className);
        ClassReader cr=new ClassReader(bytes);
        ClassNode cn=new ClassNode();
        cr.accept(cn, 0);

        for(MethodNode mn:cn.methods){
            //System.out.println(String.format("Examing Method: %s%s",mn.name,mn.desc));
            for(MethodRecord r:map){
                r.preProcess(!SkinCore.ObfuscatedEnv,obj);
                if(mn.name.equals(r.MethodName)&&mn.desc.equals(r.Desc)&&className.equals(r.ClassName)){
                    try{
                        //System.out.println(String.format("Invoking Method: %s%s",mn.name,mn.desc));
                        r.ProcessMethod.invoke(obj,mn);
                    }catch(Exception e){
                        e.printStackTrace();
                        return bytes;
                    }
                    break;
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