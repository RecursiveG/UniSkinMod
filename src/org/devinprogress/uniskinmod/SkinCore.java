package org.devinprogress.uniskinmod;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

public class SkinCore implements IFMLLoadingPlugin{
	
	@Override
	public String[] getLibraryRequestClass() {
		return null;
	}

	@Override
	public String[] getASMTransformerClass() {
		return new String[]{"org.devinprogress.uniskinmod.CoreTransformer"};
	}

	@Override
	public String getModContainerClass() {
		return "org.devinprogress.uniskinmod.CoreContainer";
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {
		System.out.println("[UniSkinMod]Starting up...");
		try {
			String cfgPath = ((File) data.get("mcLocation")).getAbsolutePath()
					+ File.separatorChar + "config" + File.separatorChar
					+ "UniSkinMod.cfg";
			String dstPath=System.getProperty("java.io.tmpdir")+File.separatorChar + 
					                           "uni_skin_mod_tmp_FjkQ908.txt";
			File f = new File(cfgPath);
			if (!f.exists()) {
				System.out.println("[UniSkinMod]Configure file do not exists. Creating...");
				f.createNewFile();
				FileWriter fw = new FileWriter(cfgPath);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write("#Skin:http://your.doman/**** ");bw.newLine();
				bw.write("#Cloak:http://your.doman/**** ");bw.newLine();
				bw.write("#Use '%s' to repesent the player's name.");bw.newLine();
				bw.write("#The file is Case-Sensitive.");bw.newLine();
				bw.flush();bw.close();fw.close();
			}
			FileReader fr = new FileReader(cfgPath);
			BufferedReader br = new BufferedReader(fr);
			FileWriter fw = new FileWriter(dstPath);
			BufferedWriter bw = new BufferedWriter(fw);
			String myreadline;
			while (br.ready()) {
				myreadline = br.readLine();
				bw.write(myreadline);
				bw.newLine();
			}
			bw.flush();bw.close();br.close();
			fw.close();br.close();fr.close();

		} catch (IOException e) {
			System.out.println("IOException");
			e.printStackTrace();
		}
	}

}
