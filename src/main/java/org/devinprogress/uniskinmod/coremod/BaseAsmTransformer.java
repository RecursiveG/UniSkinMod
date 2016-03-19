package org.devinprogress.uniskinmod.coremod;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraftforge.fml.relauncher.FMLRelaunchLog;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple class to deal with method iteration and obfuscated method names. By RecursiveG.
 */

public abstract class BaseAsmTransformer implements IClassTransformer {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    protected @interface RegisterTransformer {
        String className();

        String srgName() default "";

        String mcpName();

        String desc();
    }

    public interface IMethodTransformer {
        void transform(ClassNode cn, String classObfName,
                       MethodNode mn, String srgName,
                       boolean devEnv);
    }

    private Map<String, Map<String, IMethodTransformer>> map;
    // Map<ClassName,Map<name+desc,HandlerClass>>
    // both mcpName+desc & srgName+desc will be added.

    /* call `hookMethod` from <init>() in inherited classes */
    protected BaseAsmTransformer() {
        map = new HashMap<String, Map<String, IMethodTransformer>>();
        Class<? extends BaseAsmTransformer> clz = this.getClass();
        for (Class<?> c : clz.getDeclaredClasses()) {
            if (IMethodTransformer.class.isAssignableFrom(c)) {
                RegisterTransformer annotation = c.getAnnotation(RegisterTransformer.class);
                if (annotation == null) continue;
                Class<? extends IMethodTransformer> clzz = c.asSubclass(IMethodTransformer.class);
                try {
                    IMethodTransformer transformer = clzz.newInstance();
                    String srgName = annotation.srgName().equals("") ? annotation.mcpName() : annotation.srgName();
                    hookMethod(annotation.className(), srgName, annotation.mcpName(), annotation.desc(), transformer);
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                } catch (InstantiationException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    // param `desc` may not be obfuscated.
    protected void hookMethod(String className, String srgName, String mcpName, String desc, IMethodTransformer targetTransformer) {
        if (!map.containsKey(className))
            map.put(className, new HashMap<String, IMethodTransformer>());
        map.get(className).put(srgName + desc, targetTransformer);
        map.get(className).put(mcpName + desc, targetTransformer);
    }

    @Override
    public byte[] transform(String obfClassName, String className, byte[] bytes) {
        if (!map.containsKey(className)) return bytes;
        Map<String, IMethodTransformer> transMap = map.get(className);
        final boolean deobfuscatedEnvironment = obfClassName.equals(className);
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        // NOTE: `map` = convert obfuscated name to srgName;
        List<MethodNode> ml = new ArrayList<MethodNode>();
        ml.addAll(cn.methods);
        for (MethodNode mn : ml) {
            String methodName = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(obfClassName, mn.name, mn.desc);
            String methodDesc = FMLDeobfuscatingRemapper.INSTANCE.mapMethodDesc(mn.desc);
            if (transMap.containsKey(methodName + methodDesc)) {
                try {
                    FMLRelaunchLog.info("Transforming method %s in class %s(%s)", methodName + methodDesc, obfClassName, className);
                    transMap.get(methodName + methodDesc).transform(cn, obfClassName, mn, methodName, deobfuscatedEnvironment);
                    FMLRelaunchLog.info("Successfully transformed method %s in class %s(%s)", methodName + methodDesc, obfClassName, className);
                } catch (Exception e) {
                    FMLRelaunchLog.warning("An error happened when transforming method %s in class %s(%s). The whole class was not modified.", methodName + methodDesc, obfClassName, className);
                    e.printStackTrace();
                    return bytes;
                }
            }
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    protected static AbstractInsnNode getNthInsnNode(MethodNode mn, int opcode, int N) {
        AbstractInsnNode n = mn.instructions.getFirst();
        int count = 0;
        while (n != null) {
            if (n.getOpcode() == opcode) {
                count++;
                if (count == N)
                    break;
            }
            n = n.getNext();
        }
        return n;
    }

    protected static LabelNode getFirstLabelNode(MethodNode mn) {
        AbstractInsnNode n = mn.instructions.getFirst();
        while (n != null)
            if (n instanceof LabelNode)
                return (LabelNode) n;
        return null;
    }

    protected static AbstractInsnNode getInsnPutField(MethodNode mn, String fieldName, int count) {
        AbstractInsnNode n = mn.instructions.getFirst();
        int idx = 0;
        while (n != null) {
            if (n instanceof FieldInsnNode) {
                FieldInsnNode tmp = (FieldInsnNode) n;
                if (tmp.getOpcode() == Opcodes.PUTFIELD && tmp.name.equals(fieldName))
                    if (++idx == count)
                        return n;
            }
            n = n.getNext();
        }
        return null;
    }

    protected static void printMethod(MethodNode mn, AbstractInsnNode start, AbstractInsnNode end) {
        if (start == null) start = mn.instructions.getFirst();
        Printer printer = new Textifier();
        TraceMethodVisitor mp = new TraceMethodVisitor(printer);
        while (start != end) {
            start.accept(mp);
            start = start.getNext();
        }
        StringWriter sw = new StringWriter();
        printer.print(new PrintWriter(sw));
        printer.getText().clear();
        System.out.println(sw.toString());
    }
}