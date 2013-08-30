package org.devinprogress.uniskinmod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
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
	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		
		String TargetClass = "net.minecraft.client.entity.abstractClientPlayer";
		if (transformedName.equalsIgnoreCase(TargetClass)) {			
			System.out.println("[UniSkinMod]"+name);
			System.out.println("[UniSkinMod]"+transformedName);
			//Initialization Target Method String
			String TargetMethodSkin = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(
					"net/minecraft/client/entity/AbstractClientPlayer",
					"func_110300_d", "(Ljava/lang/String;)Ljava/lang/String;");
			String TargetMethodCloak=FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(
					"net/minecraft/client/entity/AbstractClientPlayer",
					"func_110308_e", "(Ljava/lang/String;)Ljava/lang/String;");
			System.out.println("[UniSkinMod]"+TargetMethodSkin);
			System.out.println("[UniSkinMod]"+TargetMethodCloak);
			//Load configuration file
			List<String> SkinUrl=new ArrayList<String>();
			List<String> CloakUrl=new ArrayList<String>();
			try{
			String tmpfile=System.getProperty("java.io.tmpdir")+ File.separatorChar + "uni_skin_mod_tmp_FjkQ908.txt";
			FileReader fr = new FileReader(tmpfile);
			BufferedReader br = new BufferedReader(fr); 
			String[] line;
			
			while (br.ready()) {
				line = br.readLine().split(":", 2).clone();
				if (line[0].equals("Skin")||line[0].equals("Cloak"))
					System.out.println("[UniSkinMod]URL:"+line[1]+" Added.");
				if (line[0].equals("Skin")) SkinUrl.add(line[1]);
				if (line[0].equals("Cloak")) CloakUrl.add(line[1]);				
			}
			br.close();fr.close();File f = new File(tmpfile);f.delete();}catch(IOException e){}
			
			ClassReader classReader = new ClassReader(bytes);
			ClassNode classNode = new ClassNode();
			classReader.accept(classNode,0);
			
			//Insert New Method
			MethodNode newMethod=new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,
					                             "CheckAvailability",
					                             "(Ljava/lang/String;)Z",
					                             null,null);
			LabelNode TryStart=new LabelNode(new Label());
			LabelNode TryEnd=new LabelNode(new Label());
			LabelNode TryHandle=new LabelNode(new Label());
			LabelNode IfJump=new LabelNode(new Label());
			newMethod.instructions.add(TryStart);
			newMethod.instructions.add(new TypeInsnNode(Opcodes.NEW,"java/net/URL"));
			newMethod.instructions.add(new InsnNode(Opcodes.DUP));
			newMethod.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));
			newMethod.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,"java/net/URL","<init>","(Ljava/lang/String;)V"));
			newMethod.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,"java/net/URL","openConnection","()Ljava/net/URLConnection;"));
			newMethod.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST,"java/net/HttpURLConnection"));
			newMethod.instructions.add(new VarInsnNode(Opcodes.ASTORE,1));
			newMethod.instructions.add(new VarInsnNode(Opcodes.ALOAD,1));
			newMethod.instructions.add(new LdcInsnNode("GET"));
			newMethod.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,"java/net/HttpURLConnection","setRequestMethod","(Ljava/lang/String;)V"));
			newMethod.instructions.add(new VarInsnNode(Opcodes.ALOAD,1));
			newMethod.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,"java/net/HttpURLConnection","connect","()V"));
			newMethod.instructions.add(new IntInsnNode(Opcodes.SIPUSH,200));
			newMethod.instructions.add(new VarInsnNode(Opcodes.ALOAD,1));
			newMethod.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,"java/net/HttpURLConnection","getResponseCode","()I"));
			newMethod.instructions.add(new JumpInsnNode(Opcodes.IF_ICMPNE,IfJump));
			newMethod.instructions.add(new InsnNode(Opcodes.ICONST_1));
			newMethod.instructions.add(new InsnNode(Opcodes.IRETURN));
			newMethod.instructions.add(IfJump);
			newMethod.instructions.add(new InsnNode(Opcodes.ICONST_0));
			newMethod.instructions.add(TryEnd);
			newMethod.instructions.add(new InsnNode(Opcodes.IRETURN));
			newMethod.instructions.add(TryHandle);
			newMethod.instructions.add(new VarInsnNode(Opcodes.ASTORE,1));
			newMethod.instructions.add(new InsnNode(Opcodes.ICONST_0));
			newMethod.instructions.add(new InsnNode(Opcodes.IRETURN));
			newMethod.tryCatchBlocks.add(new TryCatchBlockNode(TryStart,TryEnd,TryHandle,"java/io/IOException"));
			classNode.methods.add(newMethod);
			
			//Searching for methods
			for (MethodNode mnode : classNode.methods) {
				if (mnode.name.equals(TargetMethodCloak)&&mnode.desc.equals("(Ljava/lang/String;)Ljava/lang/String;")) {
					String SUClass="",SCCFunc="",SCCDesc="";
					for (AbstractInsnNode inode:mnode.instructions.toArray()){
						if (inode.getOpcode()==Opcodes.INVOKESTATIC)
						{
							SUClass=((MethodInsnNode)inode).owner;
							SCCFunc=((MethodInsnNode)inode).name;
							SCCDesc=((MethodInsnNode)inode).desc;
							break;
						}
					}
					mnode.instructions.clear();
					mnode.instructions.add(new InsnNode(Opcodes.ICONST_1));
					mnode.instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY,"java/lang/Object"));
					mnode.instructions.add(new InsnNode(Opcodes.DUP));
					mnode.instructions.add(new InsnNode(Opcodes.ICONST_0));
					mnode.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));
					mnode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,SUClass,SCCFunc,SCCDesc));
					mnode.instructions.add(new InsnNode(Opcodes.AASTORE));
					mnode.instructions.add(new VarInsnNode(Opcodes.ASTORE,1));
					for (String url : CloakUrl)
					{
						System.out.println("[UniSkinMod]Adding url");
						LabelNode IFJmp=new LabelNode(new Label());
						mnode.instructions.add(new LdcInsnNode(url));
						mnode.instructions.add(new VarInsnNode(Opcodes.ALOAD,1));
						mnode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"java/lang/String","format",
								                                   "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"));
						mnode.instructions.add(new VarInsnNode(Opcodes.ASTORE,2));
						mnode.instructions.add(new VarInsnNode(Opcodes.ALOAD,2));
						mnode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,name.replace(".", "/"),
								                                        "CheckAvailability","(Ljava/lang/String;)Z"));
						mnode.instructions.add(new JumpInsnNode(Opcodes.IFEQ,IFJmp));
						mnode.instructions.add(new VarInsnNode(Opcodes.ALOAD,2));
						mnode.instructions.add(new InsnNode(Opcodes.ARETURN));
						mnode.instructions.add(IFJmp);
					}

					mnode.instructions.add(new LdcInsnNode("http://skins.minecraft.net/MinecraftCloaks/%s.png"));
					mnode.instructions.add(new VarInsnNode(Opcodes.ALOAD,1));
					mnode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"java/lang/String","format",
                                        "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"));
					mnode.instructions.add(new InsnNode(Opcodes.ARETURN));
				}
				
				if (mnode.name.equals(TargetMethodSkin)&&mnode.desc.equals("(Ljava/lang/String;)Ljava/lang/String;")) {
					String SUClass="",SCCFunc="",SCCDesc="";
					for (AbstractInsnNode inode:mnode.instructions.toArray()){
						if (inode.getOpcode()==Opcodes.INVOKESTATIC)
						{
							SUClass=((MethodInsnNode)inode).owner;
							SCCFunc=((MethodInsnNode)inode).name;
							SCCDesc=((MethodInsnNode)inode).desc;
							break;
						}
					}
					System.out.println("[UniSkinMod]"+SUClass);
					System.out.println("[UniSkinMod]"+SCCFunc);
					System.out.println("[UniSkinMod]"+SCCDesc);
					System.out.println("[UniSkinMod]"+name);
					System.out.println("[UniSkinMod]"+transformedName);
					mnode.instructions.clear();
					mnode.instructions.add(new InsnNode(Opcodes.ICONST_1));
					mnode.instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY,"java/lang/Object"));
					mnode.instructions.add(new InsnNode(Opcodes.DUP));
					mnode.instructions.add(new InsnNode(Opcodes.ICONST_0));
					mnode.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));
					mnode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,SUClass,SCCFunc,SCCDesc));
					mnode.instructions.add(new InsnNode(Opcodes.AASTORE));
					mnode.instructions.add(new VarInsnNode(Opcodes.ASTORE,1));
					for (String url : SkinUrl)
					{
						LabelNode IFJmp=new LabelNode(new Label());
						mnode.instructions.add(new LdcInsnNode(url));
						mnode.instructions.add(new VarInsnNode(Opcodes.ALOAD,1));
						mnode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"java/lang/String","format",
								                                   "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"));
						mnode.instructions.add(new VarInsnNode(Opcodes.ASTORE,2));
						mnode.instructions.add(new VarInsnNode(Opcodes.ALOAD,2));
						mnode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,name.replace(".", "/"),
									                                        "CheckAvailability","(Ljava/lang/String;)Z"));
						mnode.instructions.add(new JumpInsnNode(Opcodes.IFEQ,IFJmp));
						mnode.instructions.add(new VarInsnNode(Opcodes.ALOAD,2));
						mnode.instructions.add(new InsnNode(Opcodes.ARETURN));
						mnode.instructions.add(IFJmp);
					}
					mnode.instructions.add(new LdcInsnNode("http://skins.minecraft.net/MinecraftSkins/%s.png"));
					mnode.instructions.add(new VarInsnNode(Opcodes.ALOAD,1));
					mnode.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"java/lang/String","format",
                                        "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"));
					mnode.instructions.add(new InsnNode(Opcodes.ARETURN));
				}
			}
			
			System.out.println("[UniSkinMod]Skin Url Modfied");
			ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
			classNode.accept(classWriter);
			return classWriter.toByteArray();
		}
		return bytes;
	}

}
