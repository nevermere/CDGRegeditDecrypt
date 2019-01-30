package com.android.decrypt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.EmptyBorder;

public class MainFrame extends JFrame implements ActionListener {

	private static final long serialVersionUID = -8941044369517386637L;
	private JPanel contentPane;
	private JFileChooser fileChoose;
	private static JList<String> jlFileList;
	private static DefaultListModel<String> modelFileList;// 存储添加的文件路径
	private JButton btnAddFile;
	private JButton btnDeleteFile;
	private JButton btnDecrypt;
	private JButton btnMakeFile;
	private static JProgressBar progressBar;// 处理进度条

	private static ExecutorService executorThreadPool;// 文件处理线程池
	private static int totalFileSize;// 总文件数(处理1+处理2)
	private static FileFilter fileFilter;// 文件过滤器

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					UIManager.setLookAndFeel(UIManager
							.getSystemLookAndFeelClassName());
					MainFrame frame = new MainFrame();
					frame.setVisible(true);
					// 初始化
					init();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * 初始化
	 */
	private static void init() {
		// 初始化文件处理线程池
		executorThreadPool = Executors.newFixedThreadPool(100);
		// 初始化文件过滤器
		fileFilter = new FileFilter() {

			@Override
			public boolean accept(File pathname) {
				boolean ret = true;
				if (pathname.isDirectory()) {
					for (String reg : Constant.EXCLUDE_FOLDERS) {
						if (pathname.getName().toLowerCase().matches(reg)) {
							ret = false;
							break;
						}
					}
				}
				return ret;
			}
		};
	}

	/**
	 * Create the frame.
	 * 
	 * @throws UnsupportedLookAndFeelException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws ClassNotFoundException
	 */
	public MainFrame() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		// 文件选择器,初始目录定为c盘
		fileChoose = new JFileChooser();
		fileChoose.setCurrentDirectory(new File("C://"));
		// 设置可多选
		fileChoose.setMultiSelectionEnabled(true);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 10, 413, 182);
		scrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

		// 执行进度展示
		modelFileList = new DefaultListModel<String>();
		jlFileList = new JList<String>(modelFileList);
		scrollPane.setViewportView(jlFileList);
		contentPane.add(scrollPane, BorderLayout.CENTER);

		// 处理进度条
		progressBar = new JProgressBar();
		progressBar.setForeground(Color.BLUE);
		progressBar.setStringPainted(true);
		progressBar.setBounds(10, 206, 413, 14);
		contentPane.add(progressBar);

		// 添加文件按钮
		btnAddFile = new JButton("添加文件");
		btnAddFile.setBounds(10, 230, 93, 23);
		contentPane.add(btnAddFile);
		btnAddFile.addActionListener(this);

		// 删除文件按钮
		btnDeleteFile = new JButton("删除文件");
		btnDeleteFile.setBounds(113, 230, 93, 23);
		contentPane.add(btnDeleteFile);
		btnDeleteFile.addActionListener(this);

		// 复制文件按钮1
		btnDecrypt = new JButton("Decrypt");
		btnDecrypt.setBounds(227, 229, 93, 23);
		contentPane.add(btnDecrypt);
		btnDecrypt.addActionListener(this);

		// 复制文件按钮2
		btnMakeFile = new JButton("MakeFile");
		btnMakeFile.setBounds(330, 229, 93, 23);
		contentPane.add(btnMakeFile);
		btnMakeFile.addActionListener(this);
	}

	/**
	 * 处理按钮点击事件
	 * 
	 * @param e
	 */
	public void actionPerformed(ActionEvent e) {
		if (btnAddFile.equals(e.getSource())) {// 弹出文件选择
			// 设定可以选择到文件及文件夹
			fileChoose.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			// 此句是打开文件选择器界面的触发语句
			int state = fileChoose.showOpenDialog(null);
			if (JFileChooser.CANCEL_OPTION == state) {
				return;// 撤销则返回
			} else {
				// 获取选择的文件列表
				File[] addFiles = fileChoose.getSelectedFiles();
				if (addFiles == null || addFiles.length == 0) {
					return;
				}
				// 执行文件添加操作
				executorThreadPool.execute(new FileAdd(addFiles));
			}
		} else if (btnDeleteFile.equals(e.getSource())) {// 删除当前所选文件
			// 获取当前选择的所有文件
			List<String> seleteFiles = jlFileList.getSelectedValuesList();
			if (seleteFiles != null && seleteFiles.size() != 0) {
				// 执行删除操作
				executorThreadPool.execute(new FileDelete(seleteFiles));
			}
		} else if (btnDecrypt.equals(e.getSource())) {
			// 获取当前所有添加的文件路径
			ListModel<String> fileListModel = jlFileList.getModel();
			if (fileListModel.getSize() == 0) {
				// 弹出对话框
				JOptionPane.showMessageDialog(null, "您还未选择目标文件！", "提示", 2);
				return;
			}
			try {
				// 重置进度条
				progressBar.setValue(0);
				// 循环操作
				File file;
				for (int index = 0, size = fileListModel.getSize(); index < size; index++) {
					// 打开所选文件
					file = new File(fileListModel.getElementAt(index)
							.toString());
					// 执行解密复制操作(xxx.java ---> xxx.java_)
					executorThreadPool.execute(new FileCopy(file));
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		} else if (btnMakeFile.equals(e.getSource())) {
			// 获取当前所有添加的文件路径
			ListModel<String> fileListModel = jlFileList.getModel();
			if (fileListModel.getSize() == 0) {
				// 弹出对话框
				JOptionPane.showMessageDialog(null, "您还未选择目标文件！", "提示", 2);
				return;
			}
			try {
				// 重置进度条
				progressBar.setValue(0);
				// 循环操作
				File file;
				for (int index = 0, size = fileListModel.getSize(); index < size; index++) {
					// 打开所选文件
					file = new File(fileListModel.getElementAt(index)
							.toString());
					// 执行还原复制操作(xxx.java_ ->xxx.java)
					executorThreadPool.execute(new FileCopyBack(file));
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 执行解密复制操作(xxx.java ---> xxx.java_)
	 * 
	 * @author Administrator
	 * 
	 */
	private static class FileCopy implements Runnable {

		private File fromFile;

		private FileCopy(File file) {
			this.fromFile = file;
		}

		@Override
		public void run() {
			try {
				doFileCopy(fromFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 文件扫描添加
	 * 
	 * @author Administrator
	 * 
	 */
	private static class FileAdd implements Runnable {

		private File[] files;

		private FileAdd(File[] files) {
			this.files = files;
		}

		@Override
		public void run() {
			// 选择的文件列表不为空
			for (File addFile : files) {
				// 添加所选择的文件或者文件夹到列表
				modelFileList.addElement(addFile.getAbsolutePath());
				// 更新需处理文件
				updateSubFileSize(addFile, true);

				jlFileList.setModel(modelFileList);
				System.out.println("totalFileSize:" + totalFileSize);

				// 设置进度条总数(可执行复制操作的文件总数目)
				progressBar.setMaximum(totalFileSize);
			}
		}
	}

	/**
	 * 文件删除
	 * 
	 * @author Administrator
	 * 
	 */
	private static class FileDelete implements Runnable {

		private List<String> fileList;

		private FileDelete(List<String> fileList) {
			this.fileList = fileList;
		}

		@Override
		public void run() {
			// 循环执行删除操作
			File seleteFile;
			for (String seleteFilePath : fileList) {
				modelFileList.removeElement(seleteFilePath);
				seleteFile = new File(seleteFilePath);
				// 更新需处理文件
				updateSubFileSize(seleteFile, false);

				// 更新
				jlFileList.setModel(modelFileList);
				// 设置进度条总数(可执行复制操作的文件总数目)
				progressBar.setMaximum(totalFileSize);
			}
		}

	}

	/**
	 * 执行解密复制操作(xxx.java ---> xxx.java_)
	 * 
	 * @author Administrator
	 * 
	 */
	private static class FileCopyBack implements Runnable {

		private File copyBackFile;

		private FileCopyBack(File file) {
			this.copyBackFile = file;
		}

		@Override
		public void run() {
			try {
				doFileCopyBack(copyBackFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 执行解密复制操作(xxx.java ---> xxx.java_)
	 * 
	 * @param copyFile
	 *            执行复制操作生成的文件
	 * @throws Exception
	 */
	private static void doFileCopy(File fromFile) throws Exception {
		// 复制的文件为文件夹,则复制文件夹内的文件
		if (fromFile.isDirectory()) {
			File[] files = getSubFile(fromFile);
			// 文件不为空,循环执行复制操作
			if (files != null && files.length != 0) {
				for (File file : files) {

					doFileCopy(file);
				}
			}
		} else {
			// 复制的为文件,则创建新文件(原文件路径中)
			String fromAbs = fromFile.getAbsolutePath();
			// 判断是否为需执行文件类型
			if (checkFile(fromAbs, Constant.ENCODE_FILES)) {
				// 新文件路径为:原文件路径 + "_"
				String toAbs = fromAbs + "_";
				// System.out.println("原文件路径：" + fromAbs + "\n");
				// System.out.println("新文件路径：" + toAbs + "\n");
				// 创建新文件
				File toFile = new File(toAbs);
				if (!toFile.getParentFile().exists()) {
					toFile.getParentFile().mkdirs();
				}

				System.out.println("执行解密复制操作:" + toFile.getAbsolutePath());

				// (xxx.java ---> xxx.java_)
				if (excuteCMD(fromAbs, toAbs)) {
					// 复制成功,删除原文件;失败则不处理
					fromFile.delete();
					// System.out.println("复制成功,删除原文件：" + fromAbs + "\n");
					// 更新执行进度
					int progress = progressBar.getValue() + 1;
					progressBar.setValue(progress);
				}
			}
		}
	}

	/**
	 * 执行还原复制操作(xxx.java_ ---> xxx.java)
	 * 
	 * @param copyFile
	 *            执行复制操作生成的文件
	 * @throws Exception
	 */
	private static void doFileCopyBack(File copyFile) throws Exception {
		if (copyFile.isDirectory()) {
			File[] files = getSubFile(copyFile);
			// 文件不为空,循环执行复制操作
			if (files != null && files.length != 0) {
				for (File file : files) {
					doFileCopyBack(file);
				}
			}
		} else {
			// 复制的为文件,则创建新文件(原文件路径中)
			String copyAbs = copyFile.getAbsolutePath();
			// 新文件路径为:原文件路径 + "_"
			String backAbs = copyAbs;
			// 判断是否为需执行文件类型
			if (checkFile(copyAbs, Constant.DECODE_FILES)) {
				// 复制文件路径去除尾部"_",即为原文件路径
				backAbs = copyAbs.substring(0, copyAbs.length() - 1);
				// System.out.println("新文件路径：" + copyAbs + "\n");
				// System.out.println("还原文件路径：" + backAbs + "\n");
				// 创建还原的文件
				File toFile = new File(backAbs);
				if (!toFile.getParentFile().exists()) {
					toFile.getParentFile().mkdirs();
				}

				System.out.println("执行还原复制操作:" + toFile.getAbsolutePath());

				// (xxx.java_---> xxx.java),复制成功,则删除原文件;失败则不处理
				if (excuteCMD(copyAbs, backAbs)) {
					copyFile.delete();
					// System.out.println("Decrypt完成:" + backAbs + "\n");
					// 更新执行进度
					int progress = progressBar.getValue() + 1;
					progressBar.setValue(progress);
				}
			}
		}
	}

	/**
	 * 执行CMD命令
	 * 
	 * @param fromAbs
	 * @param toAbs
	 * @return
	 * @throws Exception
	 */
	private static boolean excuteCMD(String fromAbs, String toAbs)
			throws Exception {
		String cmd1 = "cmd /c copy " + fromAbs + " " + toAbs;
		Runtime runtime = Runtime.getRuntime();
		Process process = runtime.exec(cmd1);
		// 判断是否执行成功
		InputStream input = process.getInputStream();
		// 执行失败
		if (input == null) {
			return false;
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(input,
				"GBK"));
		String line = null;
		boolean isExcuteOk = false;
		while ((line = br.readLine()) != null) {
			isExcuteOk = line.contains("已复制");
			// System.out.println("执行CMD命令结果:" + line);
		}
		input.close();
		return isExcuteOk;
	}

	/**
	 * 更新文件夹内可执行复制操作的文件数目
	 * 
	 * @param file
	 *            文件或文件夹
	 * @param doFlag
	 *            操作标识(true:添加 false:删除)
	 */
	private static void updateSubFileSize(File file, boolean doFlag) {
		if (file.isDirectory()) {
			File[] files = getSubFile(file);
			// 文件不为空,循环执行复制操作
			if (files != null && files.length != 0) {
				for (File f : files) {
					updateSubFileSize(f, doFlag);
				}
			}
		} else {
			// 判断文件是否为需执行复制操作的文件
			if (checkFile(file.getAbsolutePath(), Constant.ENCODE_FILES)) {
				if (doFlag) {
					// 添加操作
					totalFileSize += 1;
				} else {
					// 删除操作
					if (totalFileSize > 0) {
						totalFileSize -= 1;
					}
				}
			}
		}
	}

	/**
	 * 判断文件合法性
	 * 
	 * @param reg
	 * @param regs
	 * @return
	 */
	private static boolean checkFile(String filePath, String[] regs) {
		for (String reg : regs) {
			if (filePath.toLowerCase().matches(reg)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 获取文件夹内可执行复制操作的文件列表
	 * 
	 * @param file
	 * @return
	 */
	private static File[] getSubFile(File file) {
		return file.listFiles(fileFilter);
	}
}
