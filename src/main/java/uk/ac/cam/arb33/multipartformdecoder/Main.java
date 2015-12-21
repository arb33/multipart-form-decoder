/* Copyright 2015 Alastair R. Beresford
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.arb33.multipartformdecoder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.fileupload.MultipartStream;

public class Main {
	
	/*
	 * Parse out key-value pairs in header, leaving subkeys and subvalues as a single string.
	 */
	private static Map<String, String> parseHeader(String definition) {

		//Example...
		//Content-disposition: value; key1="val1"; key2="val2"\r\nContent-type: text/plain\r\n\r\n"
		String[] keyValuePairs = definition.split("\r\n");		
		Map<String, String> result = new HashMap<String, String>();
		for(String aPair : keyValuePairs) {
			int split = aPair.indexOf(":");
			if (split > 0) {
				String key = aPair.substring(0, split);
				String value = aPair.substring(split + 1, aPair.length());
				result.put(key, value);
			}
		}
		return result;
	}
	
	/*
	 * Extract the boundary from input stream; stream is unmodified by this operation.
	 *
	 */
	private static byte[] getBoundary(BufferedInputStream in) throws IOException {
		
		//Assume boundary is within first 1024 bytes
		byte[] tmp = new byte[1024];
		in.mark(tmp.length + 2);
		
		//boundary is sequence of ASCII characters with prefix "--" and terminated with CRLF
		if (in.read() != MultipartStream.DASH || in.read() != MultipartStream.DASH) {
			in.reset();
			throw new IOException("Invalid format for MultipathStream");
		}
		
		int bytesRead = 0;
		int emptyPosition = 0;
		while((bytesRead = in.read(tmp, 0, tmp.length - emptyPosition)) != -1) {
			for(int i = 0; i < tmp.length - 1; i++) {
				if (tmp[i] == MultipartStream.CR && tmp[i + 1] == MultipartStream.LF) {
					byte[] boundary = new byte[i];
					System.arraycopy(tmp, 0, boundary, 0, i);
					in.reset();
					return boundary;
				}
			}
			emptyPosition += bytesRead;
		}
		
		in.reset();
		throw new IOException(
				"Could not find any boundary definition in first " + tmp.length + " bytes");
	}
	
	
	private static void printUsage() {
		System.out.println("Extracts contents of a packet trace of a multipart/form-data HTTP POST.");
		System.out.println("Usage: java " + 
				Main.class.getCanonicalName() + " inputfile [outputfile] [outputfile] ..." + 
				"\n where outputfile is the name of the files to be overwritten " + 
				"with the contents\n of files found in the packet trace.");
	}
	
	public static void main(String[] args) throws IOException  {

		if (args.length == 0) {
			printUsage();
			return;
		}
		
		int outputFileIndex = 1;
		BufferedInputStream in = new BufferedInputStream(new FileInputStream(args[0]));
		byte[] boundary = getBoundary(in);
		MultipartStream multipartStream = new MultipartStream(in, boundary, 8192, null);
		boolean nextPart = multipartStream.skipPreamble();

		while(nextPart) {
			
			String header = multipartStream.readHeaders();
			Map<String,String> keyValuePairs = parseHeader(header);
			System.out.println(keyValuePairs.get("Content-Disposition"));
			
			if (keyValuePairs.containsKey("Content-Disposition") && 
					keyValuePairs.get("Content-Disposition").contains("filename=\"")) {
				if(outputFileIndex < args.length) {
					System.out.println(" -> Saving data to " + args[outputFileIndex]);
					BufferedOutputStream out = 
							new BufferedOutputStream(new FileOutputStream(args[outputFileIndex]));
					multipartStream.readBodyData(out);
					outputFileIndex++;
				} else {
					System.out.println(" -> Cannot save file data: insufficent input files specified.");
					multipartStream.discardBodyData();
				}
			} else {
				ByteArrayOutputStream out = (new ByteArrayOutputStream());
				multipartStream.readBodyData(out);
				System.out.println(" -> " + new String(out.toByteArray()));
			}
			nextPart = multipartStream.readBoundary();
		}
	}
}
