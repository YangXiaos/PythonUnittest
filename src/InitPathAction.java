import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;

public class InitPathAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        Project project = e.getData(PlatformDataKeys.PROJECT);
        initBasePath(project);
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

}
