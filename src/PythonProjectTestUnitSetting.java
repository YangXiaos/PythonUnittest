import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyFunction;
import com.jetbrains.python.psi.impl.PyFileImpl;

public class PythonProjectTestUnitSetting extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {

        System.out.println("喵喵喵");
        PsiFile psiFile = e.getData(PlatformDataKeys.PSI_FILE);
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
//        editor.getCaretModel().moveToOffset(2000);
        assert editor != null;

        PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(e.getProject());
        propertiesComponent.setValue("pycharm_unit_test", "喵喵喵");

        String pycharmUnitTest = propertiesComponent.getValue("pycharm_unit_test");
        System.out.println(pycharmUnitTest);
        TestUnitSetting persistDemo = ServiceManager.getService(TestUnitSetting.class);
        persistDemo.setMyState("喵喵喵");
        System.out.println(persistDemo.getState());;
        PyClass[] classes = ((PyFileImpl) psiFile).findChildrenByClass(PyClass.class);


        if (psiFile == null) {

            return;
        }

        if (psiFile.getVirtualFile().getName().startsWith("test_")) {

        }

    }
}
