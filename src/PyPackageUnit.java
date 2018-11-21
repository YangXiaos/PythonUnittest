import com.intellij.psi.PsiDirectory;
import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * python 包工具
 */
public class PyPackageUnit {


    /**
     * 包路径
     * @param packageName 包名
     */
    public static PsiDirectory createPyPackage(String packageName, PsiDirectory directory) {
        StringUtils.strip(packageName, "/");
        PsiDirectory subdirectory = directory.findSubdirectory(packageName);
        if (subdirectory == null) {
             subdirectory = directory.createSubdirectory(packageName);
        }
        if (subdirectory.findFile("__init__.py") == null)
            subdirectory.createFile("__init__.py");
        return subdirectory;
    }


    /**
     * 创建包名
     * @param packagePath 包路径
    * @param directory psi Directory
     */
    public static PsiDirectory createPyPackageOfPath(String packagePath, PsiDirectory directory) {
        for (String packageName : StringUtils.strip(packagePath, "/").split("/")) {
            directory = createPyPackage(packageName, directory);
        }
        return directory;
    }

    public static void main(String[] args) {
        String name = "test_dio.py";
        Pattern pattern = Pattern.compile("test_(.*)\\.py");
        Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {

            System.out.println(matcher.group(1));
        }
    }

}
