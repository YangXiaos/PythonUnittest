import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.PyFunction;
import org.jetbrains.annotations.NotNull;

public class PsiFileUnit
{

    /**
     * 获取 psi 文件名
     * @param file psifile
     * @return psf 文件名
     */
    public static String getPsiFileName(PsiFile file) {
        return file.getVirtualFile().getName();
    }

    /**
     * 获取 psf 文件路径
     * @param file psf file
     * @return psf 文件路径
     */
    public static String getPsiFilePath(PsiFile file) {
        return file.getVirtualFile().getPath();
    }


    /**
     * 获取 psi文件 目录
     * @param file psi文件
     */
    public static PsiDirectory getPsiFileDirectory(PsiFile file) {
        return file.getParent();
    }


    /**
     * 获取插入符 父级元素 element
     * @param editor 编辑器
     * @param psiFile psi文件
     * @param aClass 查找类
     * @param <T> ~ 元素类型
     * @return 插入符 选在块父级元素
     */
    public static <T extends PsiElement> T findCaretParentElement(Editor editor, PsiFile psiFile, Class<T> aClass) {
        PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());
        return PsiTreeUtil.getParentOfType(element, aClass);
    }


    /**
     * 找到子元素 的
     * @param editor 编辑器
     * @param psiFile psi文件
     * @param aClass 查找类
     * @param <T> ~ 元素类型
     * @return 插入符 选在块父级元素
     */
    public static <T extends PsiElement> T findCaretTopParentElement(Editor editor, PsiFile psiFile, Class<T> aClass) {

        T lastParentElement;
        T parentElement = findCaretParentElement(editor, psiFile, aClass);

        do {
            lastParentElement = parentElement;
            parentElement = findCaretParentElement(editor, psiFile, aClass);

        } while (parentElement != null);

        return lastParentElement;
    }


}
