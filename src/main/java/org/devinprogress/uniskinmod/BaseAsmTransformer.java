package org.devinprogress.uniskinmod;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraftforge.fml.relauncher.FMLRelaunchLog;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple class to deal with method iteration and obfuscated method names. By RecursiveG.
 */

public abstract class BaseAsmTransformer implements IClassTransformer{

    public interface IMethodTransformer{
        public void transform(MethodNode mn,String srgName,boolean devEnv,String classObfName);
    }

    private Map<String,Map<String,IMethodTransformer>> map;
    // Map<ClassName,Map<name+desc,HandlerClass>>
    // both mcpName+desc & srgName+desc will be added.

    /* call `hookMethod` from <init>() in inherited classes */
    protected BaseAsmTransformer() {
        map=new HashMap<String, Map<String,IMethodTransformer>>();
    }

    // param `desc` may not be obfuscated.
    protected void hookMethod(String className,String srgName,String mcpName,String desc,IMethodTransformer targetTransformer){
        if(!map.containsKey(className))
            map.put(className,new HashMap<String, IMethodTransformer>());
        map.get(className).put(srgName + desc, targetTransformer);
        map.get(className).put(mcpName + desc, targetTransformer);
    }

    @Override
    public byte[] transform(String obfClassName,String className,byte[] bytes){
        if(!map.containsKey(className))return bytes;
        Map<String,IMethodTransformer> transMap=map.get(className);
        final boolean DEV_ENV=obfClassName.equals(className);
        ClassReader cr=new ClassReader(bytes);
        ClassNode cn=new ClassNode();
        cr.accept(cn, 0);

        // NOTE: `map` = convert obfuscated name to srgName;
        for(MethodNode mn:cn.methods){
            String methodName=FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(obfClassName,mn.name,mn.desc);
            String methodDesc=FMLDeobfuscatingRemapper.INSTANCE.mapMethodDesc(mn.desc);
            if(transMap.containsKey(methodName+methodDesc)){
                try{
                    transMap.get(methodName+methodDesc).transform(mn, methodName, DEV_ENV, obfClassName);
                    FMLRelaunchLog.info("Successfully transformed method %s in class %s(%s)", methodName + methodDesc, obfClassName, className);
                }catch(Exception e){
                    FMLRelaunchLog.warning("An error happened when transforming method %s in class %s(%s). The whole class was not modified.",methodName+methodDesc, obfClassName, className);
                    e.printStackTrace();
                    return bytes;
                }
            }
        }

        ClassWriter cw=new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cn.accept(cw);
        return cw.toByteArray();
    }

    protected static AbstractInsnNode getNthInsnNode(MethodNode mn,int opcode,int N){
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

    protected static LabelNode getFirstLabelNode(MethodNode mn){
        AbstractInsnNode n=mn.instructions.getFirst();
        while(n!=null)
            if (n instanceof LabelNode)
                return (LabelNode)n;
        return null;
    }
}
