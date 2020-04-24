package tk.zeitheron.thelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Scanner;

public class THelper
{
	public static void main(String[] args)
	{
		try
		{
			File langs = new File("Localizations");
			
			final LangFile en_us = new LangFile(new File(langs, "en_us.lang"));
			
			for(File langFile : langs.listFiles(f -> f.getName().endsWith(".lang") && !f.getName().contains("en_us")))
			{
				System.out.print("Loading " + langFile.getName() + "... ");
				LangFile old = new LangFile(langFile);
				System.out.print("Processing scheme... ");
				LangFile newLang = old.applyLineScheme(en_us);
				System.out.print("Saving... ");
				newLang.save(langFile);
				System.out.println("Done!");
			}
		} catch(Throwable err)
		{
			err.printStackTrace();
		}
	}
	
	public static class LangFile
	{
		public final int lineCount;
		public final Map<Integer, String> lines = new HashMap<>();
		public final Map<Integer, Translation> translations = new HashMap<>();
		public final Map<String, String> keyToValue = new HashMap<>();
		public final Map<String, Integer> keyToLine = new HashMap<>();
		
		public LangFile(int lines)
		{
			this.lineCount = lines;
		}
		
		public LangFile(File langFile) throws FileNotFoundException
		{
			int i = 0;
			try(Scanner in = new Scanner(langFile, "UTF-8"))
			{
				while(in.hasNextLine())
				{
					String ln = in.nextLine();
					lines.put(i, ln);
					
					if(ln.trim().startsWith("#"))
					{
						++i;
						continue;
					}
					
					if(ln.contains("="))
					{
						String[] kv = ln.split("=", 2);
						translations.put(i, new Translation(kv[0]).withValue(kv[1]));
						keyToValue.put(kv[0], kv[1]);
						keyToLine.put(kv[0], i);
					}
					
					++i;
				}
			}
			this.lineCount = i;
		}
		
		public int getLineOfKey(String key)
		{
			return keyToLine.containsKey(key) ? keyToLine.get(key) : -1;
		}
		
		public LangFile applyLineScheme(LangFile from)
		{
			LangFile nf = new LangFile(from.lineCount);
			
			for(int i = 0; i < from.lineCount; ++i)
			{
				if(from.translations.containsKey(i))
				{
					Translation t = from.translations.get(i);
					String key = t.key;
					String value = keyToValue.getOrDefault(key, t.value);
					nf.lines.put(i, key + "=" + value);
					
					nf.keyToLine.put(key, i);
					nf.keyToValue.put(key, value);
					nf.translations.put(i, t.shadeKey(value));
				} else
					nf.lines.put(i, from.lines.get(i));
			}
			
			return nf;
		}
		
		public void save(File langFile) throws IOException
		{
			try(BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(langFile), StandardCharsets.UTF_8)))
			{
				for(int i = 0; i < lineCount; ++i)
				{
					if(translations.containsKey(i))
					{
						Translation t = translations.get(i);
						bw.write(t.getKey() + '=' + keyToValue.get(t.getKey()));
					} else
					{
						bw.write(lines.get(i));
					}
					
					if(i < lineCount - 1)
						bw.newLine();
				}
			}
		}
	}
	
	public static class Translation implements Entry<String, String>
	{
		public final String key;
		public String value;
		
		public Translation(String key)
		{
			this.key = key;
		}
		
		public boolean keyEquals(Translation t)
		{
			return Objects.equals(t.key, key);
		}
		
		@Override
		public String getKey()
		{
			return key;
		}
		
		@Override
		public String getValue()
		{
			return value;
		}
		
		@Override
		public String setValue(String value)
		{
			String old = this.value;
			this.value = value;
			return old;
		}
		
		public Translation withValue(String value)
		{
			setValue(value);
			return this;
		}
		
		public Translation shadeKey(String value)
		{
			return new Translation(getKey()).withValue(value);
		}
	}
}