package org.jboss.as.console.client.domain.groups.deployment;

import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.shared.deployment.DeployCommandExecutor;
import org.jboss.as.console.client.shared.deployment.DeploymentCommand;
import org.jboss.as.console.client.shared.deployment.DeploymentCommandDelegate;
import org.jboss.as.console.client.shared.deployment.TitleColumn;
import org.jboss.as.console.client.shared.model.DeploymentRecord;
import org.jboss.as.console.client.shared.viewframework.builder.MultipleToOneLayout;
import org.jboss.ballroom.client.widgets.forms.Form;
import org.jboss.ballroom.client.widgets.forms.TextAreaItem;
import org.jboss.ballroom.client.widgets.icons.Icons;
import org.jboss.ballroom.client.widgets.tables.DefaultCellTable;
import org.jboss.ballroom.client.widgets.tools.ToolButton;
import org.jboss.ballroom.client.widgets.tools.ToolStrip;

/**
 * @author Heiko Braun
 * @date 7/30/12
 */
public class ServerGroupDeploymentView {

    private DefaultCellTable<DeploymentRecord> table;
    private ListDataProvider<DeploymentRecord> dataProvider;
    private DeployCommandExecutor executor;

    public ServerGroupDeploymentView(DeployCommandExecutor executor) {
        this.executor = executor;
    }

    Widget asWidget() {


        String[] columnHeaders = new String[]{Console.CONSTANTS.common_label_name(),
                Console.CONSTANTS.common_label_runtimeName(),
                Console.CONSTANTS.common_label_enabled(),
                Console.CONSTANTS.common_label_enOrDisable(),
                Console.CONSTANTS.common_label_remove()};

        final TitleColumn titleColumn = new TitleColumn();

        TextColumn<DeploymentRecord> dplRuntimeColumn = new TextColumn<DeploymentRecord>() {
                    @Override
                    public String getValue(DeploymentRecord record) {
                        String title = null;
                        if(record.getRuntimeName().length()>27)
                            title = record.getRuntimeName().substring(0,26)+"...";
                        else
                            title = record.getRuntimeName();
                        return title;
                    }
                };


        final Column<DeploymentRecord, ImageResource> statusColumn = new Column<DeploymentRecord, ImageResource>(new ImageResourceCell()) {

            @Override
            public ImageResource getValue(DeploymentRecord deployment) {

                ImageResource res = null;

                if (deployment.isEnabled()) {
                    res = Icons.INSTANCE.status_good();
                } else {
                    res = Icons.INSTANCE.status_bad();
                }

                return res;
            }

        };

        this.table = new DefaultCellTable<DeploymentRecord>(8, new ProvidesKey<DeploymentRecord>() {
            @Override
            public Object getKey(DeploymentRecord deploymentRecord) {
                return deploymentRecord.getName();
            }

        });
        dataProvider = new ListDataProvider<DeploymentRecord>();
        dataProvider.addDataDisplay(table);

        final SingleSelectionModel<DeploymentRecord> selectionModel = new SingleSelectionModel<DeploymentRecord>();
        table.setSelectionModel(selectionModel);

        table.addColumn(titleColumn, Console.CONSTANTS.common_label_name());
        table.addColumn(dplRuntimeColumn, Console.CONSTANTS.common_label_runtimeName());
        table.addColumn(statusColumn, Console.CONSTANTS.common_label_enabled());


        Form<DeploymentRecord> form = new Form<DeploymentRecord>(DeploymentRecord.class);
        form.setNumColumns(2);
        form.setEnabled(true);
        TextAreaItem name = new TextAreaItem("name", "Name");
        TextAreaItem runtimeName = new TextAreaItem("runtimeName", "Runtime Name");
        form.setFields(name,runtimeName);

        form.bind(table);

        ToolStrip tools = new ToolStrip();
        tools.addToolButtonRight(new ToolButton("Enable/Disable", new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                new DeploymentCommandDelegate(executor, DeploymentCommand.ENABLE_DISABLE).execute(
                        selectionModel.getSelectedObject()
                );
            }
        }));
        tools.addToolButtonRight(new ToolButton("Remove", new ClickHandler() {
            @Override
            public void onClick(ClickEvent clickEvent) {
                new DeploymentCommandDelegate(executor, DeploymentCommand.REMOVE_FROM_GROUP).execute(
                        selectionModel.getSelectedObject()
                );
            }
        }));

        MultipleToOneLayout layout = new MultipleToOneLayout()
                .setPlain(true)
                .setHeadline(Console.CONSTANTS.common_label_contentRepository())
                .setMaster(Console.MESSAGES.available("Group Deployments"), table)
                .setMasterTools(tools)
                //.setDescription("The content repository contains all deployed content. Contents need to be assigned to sever groups in order to become effective (deployed).")
                .addDetail(Console.CONSTANTS.common_label_selection(), form.asWidget());

        return layout.build();
    }
}
