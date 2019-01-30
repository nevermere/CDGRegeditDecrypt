package com.android.decrypt;

public class Constant {

	/**
	 * 执行复制解密的文件类型
	 */
	public static String[] ENCODE_FILES = { "^.*\\.java$", "^.*\\.c$",
			"^.*\\.h$", "^.*\\.cpp$" };

	/**
	 * 执行复制解密的文件类型
	 */
	public static String[] DECODE_FILES = { "^.*\\.java_$", "^.*\\.c_$",
			"^.*\\.h_$", "^.*\\.cpp_$" };
	/**
	 * 不执行复制解密的文件类型
	 */
	public static String[] EXCLUDE_FOLDERS = { "^\\.svn$", "^\\.xml$",
			"^\\.png$", "^\\.jpg$" };

}
