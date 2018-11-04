/**
 * Created by admin on 2016/12/18.
 */

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@State(
        name = "setting",
        storages = {
                @Storage(value = "PluginDemo.xml")
        }
)
class TestUnitSetting implements PersistentStateComponent<String> {


    String myState;

    public String getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull String s) {
        myState = s;
    }

    public void setMyState(String state) {
        this.myState = state;
    }


}