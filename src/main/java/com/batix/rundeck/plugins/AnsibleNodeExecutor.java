package com.batix.rundeck.plugins;

import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.batix.rundeck.core.AnsibleDescribable;
import com.batix.rundeck.core.AnsibleException;
import com.batix.rundeck.core.AnsibleRunner;
import com.batix.rundeck.core.AnsibleRunnerBuilder;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.IRundeckProject;
import com.dtolabs.rundeck.core.common.ProjectManager;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;

import java.util.HashMap;
import java.util.Map;

@Plugin(name = AnsibleNodeExecutor.SERVICE_PROVIDER_NAME, service = ServiceNameConstants.NodeExecutor)
public class AnsibleNodeExecutor implements NodeExecutor, AnsibleDescribable {

  public static final String SERVICE_PROVIDER_NAME = "com.batix.rundeck.plugins.AnsibleNodeExecutor";

  public static Description DESC = null;

  static {
        DescriptionBuilder builder = DescriptionBuilder.builder();
        builder.name(SERVICE_PROVIDER_NAME);
        builder.title("Ansible Ad-Hoc Node Executor");
        builder.description("Runs Ansible Ad-Hoc commands on the nodes using the shell module.");
        builder.property(EXECUTABLE_PROP);
        builder.property(SSH_AUTH_TYPE_PROP);
        builder.property(SSH_USER_PROP);
        builder.property(SSH_PASSWORD_STORAGE_PROP);
        builder.property(SSH_KEY_FILE_PROP); 
        builder.property(SSH_KEY_STORAGE_PROP); 
        builder.property(SSH_TIMEOUT_PROP);
        builder.property(BECOME_PROP);
        builder.property(BECOME_AUTH_TYPE_PROP);
        builder.property(BECOME_USER_PROP);
        builder.property(BECOME_PASSWORD_STORAGE_PROP);
        builder.mapping(ANSIBLE_EXECUTABLE,PROJ_PROP_PREFIX + ANSIBLE_EXECUTABLE);
        builder.frameworkMapping(ANSIBLE_EXECUTABLE,FWK_PROP_PREFIX + ANSIBLE_EXECUTABLE);
        builder.mapping(ANSIBLE_SSH_AUTH_TYPE,PROJ_PROP_PREFIX + ANSIBLE_SSH_AUTH_TYPE);
        builder.frameworkMapping(ANSIBLE_SSH_AUTH_TYPE,FWK_PROP_PREFIX + ANSIBLE_SSH_AUTH_TYPE);
        builder.mapping(ANSIBLE_SSH_USER,PROJ_PROP_PREFIX + ANSIBLE_SSH_USER);
        builder.frameworkMapping(ANSIBLE_SSH_USER,FWK_PROP_PREFIX + ANSIBLE_SSH_USER);
        builder.mapping(ANSIBLE_SSH_TIMEOUT,PROJ_PROP_PREFIX + ANSIBLE_SSH_TIMEOUT);
        builder.frameworkMapping(ANSIBLE_SSH_TIMEOUT,FWK_PROP_PREFIX + ANSIBLE_SSH_TIMEOUT);
        builder.mapping(ANSIBLE_SSH_KEYPATH,PROJ_PROP_PREFIX + ANSIBLE_SSH_KEYPATH);
        builder.frameworkMapping(ANSIBLE_SSH_KEYPATH,FWK_PROP_PREFIX + ANSIBLE_SSH_KEYPATH);
        builder.mapping(ANSIBLE_SSH_KEYPATH_STORAGE_PATH,PROJ_PROP_PREFIX + ANSIBLE_SSH_KEYPATH_STORAGE_PATH);
        builder.frameworkMapping(ANSIBLE_SSH_KEYPATH_STORAGE_PATH,FWK_PROP_PREFIX + ANSIBLE_SSH_KEYPATH_STORAGE_PATH);
        builder.mapping(ANSIBLE_SSH_PASSWORD_STORAGE_PATH,PROJ_PROP_PREFIX + ANSIBLE_SSH_PASSWORD_STORAGE_PATH);
        builder.frameworkMapping(ANSIBLE_SSH_PASSWORD_STORAGE_PATH,FWK_PROP_PREFIX + ANSIBLE_SSH_PASSWORD_STORAGE_PATH);
        builder.mapping(ANSIBLE_BECOME,PROJ_PROP_PREFIX + ANSIBLE_BECOME);
        builder.frameworkMapping(ANSIBLE_BECOME,FWK_PROP_PREFIX + ANSIBLE_BECOME);
        builder.mapping(ANSIBLE_BECOME_USER,PROJ_PROP_PREFIX + ANSIBLE_BECOME_USER);
        builder.frameworkMapping(ANSIBLE_BECOME_USER,FWK_PROP_PREFIX + ANSIBLE_BECOME_USER);
        builder.mapping(ANSIBLE_BECOME_METHOD,PROJ_PROP_PREFIX + ANSIBLE_BECOME_METHOD);
        builder.frameworkMapping(ANSIBLE_BECOME_METHOD,FWK_PROP_PREFIX + ANSIBLE_BECOME_METHOD);
        builder.mapping(ANSIBLE_BECOME_PASSWORD_STORAGE_PATH,PROJ_PROP_PREFIX + ANSIBLE_BECOME_PASSWORD_STORAGE_PATH);
        builder.frameworkMapping(ANSIBLE_BECOME_PASSWORD_STORAGE_PATH,FWK_PROP_PREFIX + ANSIBLE_BECOME_PASSWORD_STORAGE_PATH);

        DESC=builder.build();
  }

  @Override
  public NodeExecutorResult executeCommand(ExecutionContext context, String[] command, INodeEntry node) {

    AnsibleRunner runner = null;

    StringBuilder cmdArgs = new StringBuilder();
    ProjectManager projectManager = context.getFramework().getProjectManager();
    IRundeckProject project = projectManager.getFrameworkProject(context.getFrameworkProject());

    cmdArgs.append("executable=").append(project.getProperty("executable"));
    for (String cmd : command) {
      cmdArgs.append(" '").append(cmd).append("'");
    }

    Map<String, Object> jobConf = new HashMap<String, Object>();
    jobConf.put(AnsibleDescribable.ANSIBLE_MODULE,"shell");
    jobConf.put(AnsibleDescribable.ANSIBLE_MODULE_ARGS,cmdArgs.toString());
    jobConf.put(AnsibleDescribable.ANSIBLE_LIMIT,node.getNodename());

    if ("true".equals(System.getProperty("ansible.debug"))) {
      jobConf.put(AnsibleDescribable.ANSIBLE_DEBUG,"True");
    } else {
      jobConf.put(AnsibleDescribable.ANSIBLE_DEBUG,"False");
    }

    AnsibleRunnerBuilder builder = new AnsibleRunnerBuilder(node, context, context.getFramework(), jobConf);

    try {
        runner = builder.buildAnsibleRunner();  
    } catch (ConfigurationException e) {
          return NodeExecutorResultImpl.createFailure(AnsibleException.AnsibleFailureReason.ParseArgumentsError, e.getMessage(), node);
    }

    try {
        runner.run();
    } catch (Exception e) {
        return NodeExecutorResultImpl.createFailure(AnsibleException.AnsibleFailureReason.AnsibleError, e.getMessage(), node);
    }

    return NodeExecutorResultImpl.createSuccess(node);
  }

  @Override
  public Description getDescription() {
    return DESC;
  }
}

