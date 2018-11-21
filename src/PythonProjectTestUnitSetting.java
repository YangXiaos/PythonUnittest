import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyFunction;
import com.jetbrains.python.psi.impl.PyFunctionBuilder;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PythonProjectTestUnitSetting extends AnAction {

    private static final Logger LOG = Logger.getLogger(PythonProjectTestUnitSetting.class);
    private static final String TEST_FUNCTION_PREFIX = "test_";
    private static final Pattern TEST_FILE_PREFIX = Pattern.compile("test_(.*)\\.py");


    @Override
    public void actionPerformed(AnActionEvent e) {


        Project project = e.getData(PlatformDataKeys.PROJECT);
        PsiFile psiFile = e.getData(PlatformDataKeys.PSI_FILE);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);

        // 判断是否为null
        if (project == null || editor == null || psiFile == null) {
            return;
        }

        PsiDirectory baseDir = PsiManager.getInstance(project).findDirectory(project.getBaseDir());

        // 获取插入符的类，函数
        PyClass pyClass = PsiFileUnit.findCaretParentElement(editor, psiFile, PyClass.class);
        PyFunction pyFunction = PsiFileUnit.findCaretParentElement(editor, psiFile, PyFunction.class);
        

        // 判断插入符元素是否为空
        if (pyClass == null && pyFunction == null) {
            LOG.info("选中符元素为空");
            return;
        }

        // 文件名，文件路径，项目路径
        String fileName = psiFile.getVirtualFile().getName();
        String filePath = psiFile.getVirtualFile().getPath();

        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        String baseSourcePath = properties.getValue("PycharmTestUnit.BaseSourcePath");
        String testUnitPath = properties.getValue("PycharmTestUnit.TestUnitPath");

        // 确认项目路径
        if (baseSourcePath == null || testUnitPath == null) {
            initBasePath(project);
            baseSourcePath = properties.getValue("PycharmTestUnit.BaseSourcePath");
            testUnitPath = properties.getValue("PycharmTestUnit.TestUnitPath");
        }

        // 相对文件路径, 相对文件夹路径
        String relativeDirFilePath = filePath.replace(baseSourcePath, "").replace(testUnitPath, "");
        String relativeDirPath = getRelativeDirPath(relativeDirFilePath);
        String sourceDirParentPath = baseSourcePath + relativeDirPath;
        String testUnitDirParentPath = testUnitPath + relativeDirPath;

        findFileOfPath(testUnitPath);

        // 测试文件 切回源文件
        if (filePath.startsWith(testUnitPath) && fileName.startsWith(TEST_FUNCTION_PREFIX)) {

            String elementName = getElementName(fileName);
            VirtualFile directory = getDirectory(sourceDirParentPath);
            if (directory == null) {
                // 提示没有 code
                return;
            }

            PsiDirectory dir = PsiManager.getInstance(project).findDirectory(directory);
            List<PsiElement> elementList = new LinkedList<>();
            elementList.addAll(getClassChildrenOfName(dir, elementName));
            elementList.addAll(getFunctionChildrenOfName(dir, elementName));
            showSourceGroup(e, elementList);
            return;
        }


        // 源文件切回 测试文件， 创建测试文件
        if (filePath.startsWith(baseSourcePath)) {

            String testFileName = "test_%s.py";
            String testFunctionName = "test_%s";

            // 获取测试文件名，测试函数名
            testFileName = pyClass != null ?
                    String.format(testFileName, pyClass.getName()) :
                    String.format(testFileName, pyFunction.getName());
            testFunctionName = pyFunction == null ?
                    String.format(testFunctionName, pyClass.getName()) : // 为Null 使用 类名
                    String.format(testFunctionName, pyFunction.getName());

            VirtualFile file = findFileOfPath(testUnitDirParentPath + "/" + testFileName);

            // 如果获取不到测试文件，则创建切换
            if (file == null) {
                // 创建测试文件
                String testRelativeDirFilePath = (testUnitPath + relativeDirFilePath).replace(project.getBasePath(), "");
                PsiDirectory relativeDir = PyPackageUnit.createPyPackageOfPath(getRelativeDirPath(testRelativeDirFilePath), baseDir);

                PsiFile testPsiFile = relativeDir.findFile(testFileName);
                PyFile testFile;
                if (testPsiFile == null) {
                    testFile = (PyFile) relativeDir.createFile(testFileName);
                } else {
                    testFile = (PyFile) testPsiFile;
                }
                // 创建测试函数
                PyFunction testFunction = new PyFunctionBuilder(testFunctionName, testFile).buildFunction();
                Runnable runnable = () -> {
                    //写入
                    testFile.add(testFunction);
                };
                WriteCommandAction.runWriteCommandAction(project, runnable);
                toggleCurrentEditor(project, testFile).getCaretModel().moveToOffset(testFunction.getTextOffset());
                return;
            }
            PsiFile testFile = PsiManager.getInstance(project).findFile(file);
            // 切换至测试文件
            Editor currentEditor = toggleCurrentEditor(project, testFile);

            // 确认是否有创建测试函数
            List<PsiElement> children = getFunctionChildrenOfName(testFile, testFunctionName);
            if (children.size() != 0) {
                currentEditor.getCaretModel().moveToOffset(children.get(0).getTextOffset());
            } else {

                PyFunction testFunction = new PyFunctionBuilder(testFunctionName, testFile).buildFunction();
                Runnable runnable = () -> {
                    //写入
                    testFile.add(testFunction);
                };
                WriteCommandAction.runWriteCommandAction(project, runnable);
                currentEditor.getCaretModel().moveToOffset(testFunction.getTextOffset());
            }
        }
    }


    /**
     * 初始化 测试单元文件路径
     */
    private void initBasePath(Project project) {
        InitBasePathDialog dialog = new InitBasePathDialog(project);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }


    /**
     * 切换窗口
     * @param project 项目
     * @param psiFile 文件
     * @return 切换的 editor
     */
    private Editor toggleCurrentEditor(Project project, PsiFile psiFile) {
        OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(project, psiFile.getVirtualFile());
        return FileEditorManager.getInstance(project).openTextEditor(openFileDescriptor, true);
    }


    /**
     * 获取 相对文件夹路径
     * @param filePath 相对文件路径
     * @return 相对文件夹路径
     */
    private String getRelativeDirPath(String filePath) {
        String[] split = filePath.split("/");
        if (split.length == 1) {
            return "";
        }
        return String.join("/", Arrays.copyOfRange(split, 0, split.length-1)) ;
    }


    /**
     * 判断是否存在 目录
     * @param dirPath 目录路径
     * @return VirtualFile
     */
    private VirtualFile getDirectory(String dirPath) {
        return LocalFileSystem.getInstance().findFileByPath(dirPath);
    }


    /**
     * 获取文件
     * @param filePath 文件路径
     * @return VirtualFile
     */
    private VirtualFile findFileOfPath(String filePath) {
        return LocalFileSystem.getInstance().findFileByPath(filePath);
    }


    /**
     * 获取 元素名
     * @param fileName 测试文件名
     * @return 元素名
     */
    private String getElementName(String fileName) {
        Matcher matcher = TEST_FILE_PREFIX.matcher(fileName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }


    /**
     * 从父级元素，获取特定类名的元素
     * @param parentElement 父级元素
     * @param name 类名
     * @return psiElement 列表
     */
    private List<PsiElement> getClassChildrenOfName(PsiElement parentElement, String name) {
        PyClass[] children = PsiTreeUtil.getChildrenOfType(parentElement, PyClass.class);

        List<PsiElement> result = new LinkedList<>();
        if (children == null) {
            return result;
        }

        for (PyClass child : children) {
            if (Objects.equals(child.getName(), name)) {
                result.add(child);
            }
        }
        return result;
    }


    /**
     * 从父级元素，获取特定类名的元素
     * @param parentElement 父级元素
     * @param name 类名
     * @return psiElement 列表
     */
    private List<PsiElement> getFunctionChildrenOfName(PsiElement parentElement, String name) {
        PyFunction[] children = PsiTreeUtil.getChildrenOfType(parentElement, PyFunction.class);

        List<PsiElement> result = new LinkedList<>();
        if (children == null) {
            return result;
        }

        for (PyFunction child : children) {
            if (Objects.equals(child.getName(), name)) {
                result.add(child);
            }
        }
        return result;
    }


    /**
     * 弹窗设定
     * @param e 事件
     * @param codeElementList 元素
     */
    private void showSourceGroup(AnActionEvent e, List<PsiElement> codeElementList) {
        DefaultActionGroup actionGroup = (DefaultActionGroup) ActionManager.getInstance().getAction("Sample_JBPopupActionGroup");
        actionGroup.removeAll();

        for (PsiElement psiElement : codeElementList) {

            // pyclass
            if (psiElement instanceof PyClass) {
                PyClass cls = (PyClass) psiElement;
                actionGroup.add(new AnAction(cls.getName()) {
                    @Override
                    public void actionPerformed(AnActionEvent e) {
                        // 获取元素所在文件
                        PsiFile psiFile = PsiTreeUtil.getParentOfType(cls, PsiFile.class);
                        // 切换文件
                        Editor editor = toggleCurrentEditor(e.getProject(), psiFile);
                        // 定位元素
                        editor.getCaretModel().moveToOffset(cls.getTextOffset());
                    }
                });
            }

            // pyfunction
            if (psiElement instanceof PyFunction) {
                PyFunction cls = (PyFunction) psiElement;
                actionGroup.add(new AnAction(cls.getName()) {
                    @Override
                    public void actionPerformed(AnActionEvent e) {
                        // 获取元素所在文件
                        PsiFile psiFile = PsiTreeUtil.getParentOfType(cls, PsiFile.class);
                        // 切换文件
                        Editor editor = toggleCurrentEditor(e.getProject(), psiFile);
                        // 定位元素
                        editor.getCaretModel().moveToOffset(cls.getTextOffset());
                    }
                });
            }

        }

        ListPopup listPopup = JBPopupFactory.getInstance()
                .createActionGroupPopup("Choose Test for ...", actionGroup, e.getDataContext(),
                        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false);

        listPopup.showInBestPositionFor(e.getDataContext());
    }

}
