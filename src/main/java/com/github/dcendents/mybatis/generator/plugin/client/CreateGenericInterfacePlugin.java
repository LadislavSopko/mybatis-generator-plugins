package com.github.dcendents.mybatis.generator.plugin.client;

import static org.mybatis.generator.internal.util.StringUtility.stringHasValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mybatis.generator.api.GeneratedJavaFile;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.DefaultJavaFormatter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.JavaVisibility;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.java.TopLevelClass;

import lombok.NoArgsConstructor;

/**
 * Mybatis generator plugin to create a generic interface for all mappers.
 */
@NoArgsConstructor
public class CreateGenericInterfacePlugin extends PluginAdapter {
	public static final String INTERFACE = "interface";
	public static final String CONTROLLER_NS = "restControlerNs";
	public static final String CONTROLLER_BASE = "restControlerBase";
	public static final String API_BASE_PATH = "restBasePath";

	private String apiBasePath;
	private String interfaceName;
	private Interface genericInterface;

	private String controllerNs;
	private String controllerBase;

	private FullyQualifiedJavaType genericModel = new FullyQualifiedJavaType("T");
	private FullyQualifiedJavaType genericExample = new FullyQualifiedJavaType("U");
	private FullyQualifiedJavaType genericId = new FullyQualifiedJavaType("V");

	private FullyQualifiedJavaType genericModelList;
	private FullyQualifiedJavaType longPrimitive;

	private Set<String> methodsAdded;

	private Map<IntrospectedTable, FullyQualifiedJavaType> models;
	private Map<IntrospectedTable, FullyQualifiedJavaType> examples;
	private Map<IntrospectedTable, FullyQualifiedJavaType> ids;
	private Map<IntrospectedTable, TopLevelClass> controllers;

	private boolean suppressAllComments = false;
	private String targetRuntime = "MyBatis3";

	@Override
	public boolean validate(List<String> warnings) {
		apiBasePath = properties.getProperty(API_BASE_PATH);

		if (!stringHasValue(interfaceName)) {
			apiBasePath = "/api/";
		}

		interfaceName = properties.getProperty(INTERFACE);

		String warning = "Property %s not set for plugin %s";
		if (!stringHasValue(interfaceName)) {
			warnings.add(String.format(warning, INTERFACE, this.getClass().getSimpleName()));
			return false;
		}

		controllerNs = properties.getProperty(CONTROLLER_NS);

		warning = "Property %s not set for plugin %s";
		if (!stringHasValue(controllerNs)) {
			warnings.add(String.format(warning, CONTROLLER_NS, this.getClass().getSimpleName()));
			return false;
		}

		controllerBase = properties.getProperty(CONTROLLER_BASE);

		warning = "Property %s not set for plugin %s";
		if (!stringHasValue(controllerBase)) {
			warnings.add(String.format(warning, CONTROLLER_BASE, this.getClass().getSimpleName()));
			return false;
		}

		init();

		return true;
	}

	private void init() {

		suppressAllComments = this.context.getCommentGeneratorConfiguration().getProperty("suppressAllComments")
				.equals("true");
		targetRuntime = this.context.getTargetRuntime();

		genericModelList = FullyQualifiedJavaType.getNewListInstance();
		genericModelList.addTypeArgument(genericModel);

		longPrimitive = new FullyQualifiedJavaType("long");

		FullyQualifiedJavaType className = new FullyQualifiedJavaType(interfaceName);
		className.addTypeArgument(genericModel);
		if (!this.targetRuntime.equals("MyBatis3DynamicSql")) {
			className.addTypeArgument(genericExample);
			className.addTypeArgument(genericId);
		}

		genericInterface = new Interface(className);
		genericInterface.setVisibility(JavaVisibility.PUBLIC);
		genericInterface.addImportedType(new FullyQualifiedJavaType("javax.annotation.Generated"));
		genericInterface.addImportedType(
				new FullyQualifiedJavaType("org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter"));
		genericInterface
				.addImportedType(new FullyQualifiedJavaType("org.mybatis.dynamic.sql.select.QueryExpressionDSL"));
		genericInterface.addImportedType(
				new FullyQualifiedJavaType("org.mybatis.dynamic.sql.delete.MyBatis3DeleteModelAdapter"));
		genericInterface.addImportedType(new FullyQualifiedJavaType("org.mybatis.dynamic.sql.delete.DeleteDSL"));
		genericInterface.addImportedType(
				new FullyQualifiedJavaType("org.mybatis.dynamic.sql.update.MyBatis3UpdateModelAdapter"));
		genericInterface.addImportedType(new FullyQualifiedJavaType("org.mybatis.dynamic.sql.update.UpdateDSL"));
		genericInterface.addImportedType(new FullyQualifiedJavaType("java.util.List"));

		methodsAdded = new HashSet<>();

		models = new HashMap<>();
		examples = new HashMap<>();
		ids = new HashMap<>();
		controllers = new HashMap<>();
	}

	@Override
	public List<GeneratedJavaFile> contextGenerateAdditionalJavaFiles() {
		List<GeneratedJavaFile> models = new ArrayList<>();

		GeneratedJavaFile genericInterfaceFile = new GeneratedJavaFile(genericInterface,
				context.getJavaClientGeneratorConfiguration().getTargetProject(), new DefaultJavaFormatter());

		models.add(genericInterfaceFile);

		// for all controllers
		for (TopLevelClass tc : controllers.values()) {
			GeneratedJavaFile genericInterfaceFileC = new GeneratedJavaFile(tc,
					context.getJavaClientGeneratorConfiguration().getTargetProject(), new DefaultJavaFormatter());

			models.add(genericInterfaceFileC);
		}

		return models;
	}

	private void generateClassForController(IntrospectedTable introspectedTable, Interface interfaze) { // table and
		String modelName = models.containsKey(introspectedTable) ? models.get(introspectedTable).getShortName()
				: "somename";

		FullyQualifiedJavaType controllerClass = new FullyQualifiedJavaType(
				controllerNs + "." + modelName + "Controller");
		FullyQualifiedJavaType baseControlerType = new FullyQualifiedJavaType(controllerBase);
		baseControlerType.addTypeArgument(models.get(introspectedTable));
		baseControlerType.addTypeArgument(interfaze.getType());

		TopLevelClass tableController = new TopLevelClass(controllerClass);
		tableController.setSuperClass(baseControlerType);
		tableController.setVisibility(JavaVisibility.PUBLIC);

		String tns = "";
		// get model namespace
		try {
			tns = context.getJavaModelGeneratorConfiguration().getTargetPackage() + "."
					+ introspectedTable.getTableConfiguration().getSchema().toLowerCase();
		} catch (Exception e) {
			// ...
		}

		tableController.addImportedType(new FullyQualifiedJavaType(tns + "." + modelName));
		tableController.addImportedType(interfaze.getType());
		if (interfaze.getType() != null) {
			tableController.addImportedType(new FullyQualifiedJavaType(
					interfaze.getType().getPackageName() + "." + modelName + "DynamicSqlSupport"));
		}

		tableController.addImportedType(new FullyQualifiedJavaType("org.apache.ibatis.session.SqlSessionFactory"));
		tableController
				.addImportedType(new FullyQualifiedJavaType("org.springframework.web.bind.annotation.CrossOrigin"));
		tableController
				.addImportedType(new FullyQualifiedJavaType("org.springframework.web.bind.annotation.RequestMapping"));
		tableController
				.addImportedType(new FullyQualifiedJavaType("org.springframework.web.bind.annotation.RestController"));
		tableController.addAnnotation("@CrossOrigin(origins = { \"*\" }, maxAge = 3600)");
		tableController.addAnnotation("@RestController");
		tableController.addAnnotation("@RequestMapping({ \"" + apiBasePath + modelName.toLowerCase() + "\" })");

		Method ctor = new Method(controllerClass.getShortName());
		ctor.setConstructor(true);
		ctor.setVisibility(JavaVisibility.PUBLIC);
		Parameter param = new Parameter(new FullyQualifiedJavaType("org.apache.ibatis.session.SqlSessionFactory"),
				"sqlSessionFactory");
		ctor.addParameter(param);

		ctor.getBodyLines().clear();
		ctor.addBodyLine(
				String.format("super(sqlSessionFactory, new %sDynamicSqlSupport.%s());", modelName, modelName));

		tableController.addMethod(ctor);
		controllers.put(introspectedTable, tableController);
	}

	@Override
	public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass,
			IntrospectedTable introspectedTable) {

		FullyQualifiedJavaType type = new FullyQualifiedJavaType(interfaceName);
		type.addTypeArgument(models.get(introspectedTable));
		if (examples.containsKey(introspectedTable)) {
			type.addTypeArgument(examples.get(introspectedTable));
		}
		if (ids.containsKey(introspectedTable)) {
			type.addTypeArgument(ids.get(introspectedTable));
		}

		interfaze.addSuperInterface(type);

		generateClassForController(introspectedTable, interfaze);

		return true;
	}

	void addGenericMethod(Method method, FullyQualifiedJavaType returnType, FullyQualifiedJavaType... types) {
		method.addAnnotation("@Override");

		if (!methodsAdded.contains(method.getName())) {
			Method genericMethod = new Method(method.getName());
			if (!this.suppressAllComments) {
				genericMethod.addJavaDocLine("/**");
				genericMethod.addJavaDocLine(" * This method was generated by MyBatis Generator.");
				genericMethod.addJavaDocLine(" *");
				genericMethod.addJavaDocLine(" * @mbg.generated");
				genericMethod.addJavaDocLine(" */");
			}
			genericMethod.addAnnotation("@Generated(\"org.mybatis.generator.api.MyBatisGenerator\")");
			genericMethod.setReturnType(returnType);
			genericMethod.setDefault(true);

			for (int i = 0; i < method.getParameters().size(); i++) {
				Parameter parameter = method.getParameters().get(i);
				FullyQualifiedJavaType paramType = types.length > i ? types[i] : parameter.getType();

				Parameter genericParameter = new Parameter(paramType, parameter.getName());
				genericMethod.addParameter(genericParameter);
			}

			genericMethod.getBodyLines().clear();
			String castType = returnType.isPrimitive() ? returnType.getPrimitiveTypeWrapper().getShortName()
					: returnType.getShortName();
			genericMethod.addBodyLine(String.format("return (%s)null;", castType));

			genericInterface.addMethod(genericMethod);

			methodsAdded.add(method.getName());
		}
	}

	@Override
	public boolean clientCountByExampleMethodGenerated(Method method, Interface interfaze,
			IntrospectedTable introspectedTable) {
		addClientCountByExample(method, introspectedTable);
		return true;
	}

	@Override
	public boolean clientCountByExampleMethodGenerated(Method method, TopLevelClass topLevelClass,
			IntrospectedTable introspectedTable) {
		addClientCountByExample(method, introspectedTable);
		return true;
	}

	private void addClientCountByExample(Method method, IntrospectedTable introspectedTable) {
		if (method.getParameters().size() > 0) {
			examples.put(introspectedTable, method.getParameters().get(0).getType());
		}

		if (this.targetRuntime.equals("MyBatis3DynamicSql")) {
			// org.mybatis.dynamic.sql.select.QueryExpressionDSL<org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter<Long>>
			FullyQualifiedJavaType type = new FullyQualifiedJavaType(
					"org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter");
			type.addTypeArgument(new FullyQualifiedJavaType("Long"));
			FullyQualifiedJavaType type1 = new FullyQualifiedJavaType(
					"org.mybatis.dynamic.sql.select.QueryExpressionDSL");
			type1.addTypeArgument(type);
			addGenericMethod(method, type1, genericExample);
		} else {
			addGenericMethod(method, longPrimitive, genericExample);
		}

	}

	@Override
	public boolean clientDeleteByExampleMethodGenerated(Method method, Interface interfaze,
			IntrospectedTable introspectedTable) {
		addClientDeleteByExample(method);
		return true;
	}

	@Override
	public boolean clientDeleteByExampleMethodGenerated(Method method, TopLevelClass topLevelClass,
			IntrospectedTable introspectedTable) {
		addClientDeleteByExample(method);
		return true;
	}

	private void addClientDeleteByExample(Method method) {

		if (this.targetRuntime.equals("MyBatis3DynamicSql")) {
			// org.mybatis.dynamic.sql.delete.DeleteDSL<org.mybatis.dynamic.sql.delete.MyBatis3DeleteModelAdapter<Integer>>
			FullyQualifiedJavaType type = new FullyQualifiedJavaType(
					"org.mybatis.dynamic.sql.delete.MyBatis3DeleteModelAdapter");
			type.addTypeArgument(new FullyQualifiedJavaType("Integer"));
			FullyQualifiedJavaType type1 = new FullyQualifiedJavaType("org.mybatis.dynamic.sql.delete.DeleteDSL");
			type1.addTypeArgument(type);

			addGenericMethod(method, type1, genericExample);
		} else {
			addGenericMethod(method, FullyQualifiedJavaType.getIntInstance(), genericExample);
		}

	}

	@Override
	public boolean clientDeleteByPrimaryKeyMethodGenerated(Method method, Interface interfaze,
			IntrospectedTable introspectedTable) {
		addClientDeleteByPrimaryKey(method, introspectedTable);
		return true;
	}

	@Override
	public boolean clientDeleteByPrimaryKeyMethodGenerated(Method method, TopLevelClass topLevelClass,
			IntrospectedTable introspectedTable) {
		addClientDeleteByPrimaryKey(method, introspectedTable);
		return true;
	}

	private void addClientDeleteByPrimaryKey(Method method, IntrospectedTable introspectedTable) {
		if (method.getParameters().size() > 0) {
			ids.put(introspectedTable, method.getParameters().get(0).getType());
		}
		addGenericMethod(method, FullyQualifiedJavaType.getIntInstance(), genericId);
	}

	@Override
	public boolean clientInsertMethodGenerated(Method method, Interface interfaze,
			IntrospectedTable introspectedTable) {
		addClientInsert(method, introspectedTable);
		return true;
	}

	@Override
	public boolean clientInsertMethodGenerated(Method method, TopLevelClass topLevelClass,
			IntrospectedTable introspectedTable) {
		addClientInsert(method, introspectedTable);
		return true;
	}

	private void addClientInsert(Method method, IntrospectedTable introspectedTable) {
		/*
		 * if (method.getParameters().size() > 0) { models.put(introspectedTable,
		 * method.getParameters().get(0).getType()); }
		 */
		addGenericMethod(method, FullyQualifiedJavaType.getIntInstance(), genericModel);
	}

	@Override
	public boolean clientSelectByExampleWithBLOBsMethodGenerated(Method method, Interface interfaze,
			IntrospectedTable introspectedTable) {
		addClientSelectByExampleWithBLOBs(method, introspectedTable);
		return true;
	}

	@Override
	public boolean clientSelectByExampleWithBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
			IntrospectedTable introspectedTable) {
		addClientSelectByExampleWithBLOBs(method, introspectedTable);
		return true;
	}

	private void addClientSelectByExampleWithBLOBs(Method method, IntrospectedTable introspectedTable) {

		if (this.targetRuntime.equals("MyBatis3DynamicSql")) {
			// org.mybatis.dynamic.sql.select.QueryExpressionDSL<org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter<List<ProgDtl>>>
			FullyQualifiedJavaType type = new FullyQualifiedJavaType(
					"org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter");
			type.addTypeArgument(genericModelList);
			FullyQualifiedJavaType type1 = new FullyQualifiedJavaType(
					"org.mybatis.dynamic.sql.select.QueryExpressionDSL");
			type1.addTypeArgument(type);

			// cache models QueryExpressionDSL<MyBatis3SelectModelAdapter<List<EmitDes>>>
			models.put(introspectedTable, method.getReturnType().getTypeArguments().get(0).getTypeArguments().get(0)
					.getTypeArguments().get(0));

			addGenericMethod(method, type1, genericExample);
		} else {
			addGenericMethod(method, genericModelList, genericExample);
		}

	}

	@Override
	public boolean clientSelectByExampleWithoutBLOBsMethodGenerated(Method method, Interface interfaze,
			IntrospectedTable introspectedTable) {
		addClientSelectByExampleWithoutBLOBs(method);
		return true;
	}

	@Override
	public boolean clientSelectByExampleWithoutBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
			IntrospectedTable introspectedTable) {
		addClientSelectByExampleWithoutBLOBs(method);
		return true;
	}

	private void addClientSelectByExampleWithoutBLOBs(Method method) {

		addGenericMethod(method, genericModelList, genericExample);
	}

	@Override
	public boolean clientSelectByPrimaryKeyMethodGenerated(Method method, Interface interfaze,
			IntrospectedTable introspectedTable) {
		addClientSelectByPrimaryKey(method);
		return true;
	}

	@Override
	public boolean clientSelectByPrimaryKeyMethodGenerated(Method method, TopLevelClass topLevelClass,
			IntrospectedTable introspectedTable) {
		addClientSelectByPrimaryKey(method);
		return true;
	}

	private void addClientSelectByPrimaryKey(Method method) {

		// this is called also for selectDistinctByExample
		if (this.targetRuntime.equals("MyBatis3DynamicSql")) {
			// org.mybatis.dynamic.sql.select.QueryExpressionDSL<org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter<List<ProgDtl>>>
			FullyQualifiedJavaType type = new FullyQualifiedJavaType(
					"org.mybatis.dynamic.sql.select.MyBatis3SelectModelAdapter");
			type.addTypeArgument(genericModelList);
			FullyQualifiedJavaType type1 = new FullyQualifiedJavaType(
					"org.mybatis.dynamic.sql.select.QueryExpressionDSL");
			type1.addTypeArgument(type);

			addGenericMethod(method, type1, genericId);
		} else {
			addGenericMethod(method, genericModel, genericId);
		}

	}

	@Override
	public boolean clientUpdateByExampleSelectiveMethodGenerated(Method method, Interface interfaze,
			IntrospectedTable introspectedTable) {
		addClientUpdateByExampleSelective(method);
		return true;
	}

	@Override
	public boolean clientUpdateByExampleSelectiveMethodGenerated(Method method, TopLevelClass topLevelClass,
			IntrospectedTable introspectedTable) {
		addClientUpdateByExampleSelective(method);
		return true;
	}

	private void addClientUpdateByExampleSelective(Method method) {

		if (this.targetRuntime.equals("MyBatis3DynamicSql")) {
			// org.mybatis.dynamic.sql.update.UpdateDSL<org.mybatis.dynamic.sql.update.MyBatis3UpdateModelAdapter<Integer>>
			FullyQualifiedJavaType type = new FullyQualifiedJavaType(
					"org.mybatis.dynamic.sql.update.MyBatis3UpdateModelAdapter");
			type.addTypeArgument(new FullyQualifiedJavaType("Integer"));
			FullyQualifiedJavaType type1 = new FullyQualifiedJavaType("org.mybatis.dynamic.sql.update.UpdateDSL");
			type1.addTypeArgument(type);

			addGenericMethod(method, type1, genericModel, genericExample);
		} else {
			addGenericMethod(method, FullyQualifiedJavaType.getIntInstance(), genericModel, genericExample);
		}

	}

	@Override
	public boolean clientUpdateByExampleWithBLOBsMethodGenerated(Method method, Interface interfaze,
			IntrospectedTable introspectedTable) {
		addClientUpdateByExampleWithBLOBs(method);
		return true;
	}

	@Override
	public boolean clientUpdateByExampleWithBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
			IntrospectedTable introspectedTable) {
		addClientUpdateByExampleWithBLOBs(method);
		return true;
	}

	private void addClientUpdateByExampleWithBLOBs(Method method) {

		if (this.targetRuntime.equals("MyBatis3DynamicSql")) {
			// org.mybatis.dynamic.sql.update.UpdateDSL<org.mybatis.dynamic.sql.update.MyBatis3UpdateModelAdapter<Integer>>
			FullyQualifiedJavaType type = new FullyQualifiedJavaType(
					"org.mybatis.dynamic.sql.update.MyBatis3UpdateModelAdapter");
			type.addTypeArgument(new FullyQualifiedJavaType("Integer"));
			FullyQualifiedJavaType type1 = new FullyQualifiedJavaType("org.mybatis.dynamic.sql.update.UpdateDSL");
			type1.addTypeArgument(type);

			addGenericMethod(method, type1, genericModel, genericExample);
		} else {
			addGenericMethod(method, FullyQualifiedJavaType.getIntInstance(), genericModel, genericExample);
		}
	}

	@Override
	public boolean clientUpdateByExampleWithoutBLOBsMethodGenerated(Method method, Interface interfaze,
			IntrospectedTable introspectedTable) {
		addClientUpdateByExampleWithoutBLOBs(method);
		return true;
	}

	@Override
	public boolean clientUpdateByExampleWithoutBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
			IntrospectedTable introspectedTable) {
		addClientUpdateByExampleWithoutBLOBs(method);
		return true;
	}

	private void addClientUpdateByExampleWithoutBLOBs(Method method) {
		addGenericMethod(method, FullyQualifiedJavaType.getIntInstance(), genericModel, genericExample);
	}

	@Override
	public boolean clientUpdateByPrimaryKeySelectiveMethodGenerated(Method method, Interface interfaze,
			IntrospectedTable introspectedTable) {
		addClientUpdateByPrimaryKeySelective(method);
		return true;
	}

	@Override
	public boolean clientUpdateByPrimaryKeySelectiveMethodGenerated(Method method, TopLevelClass topLevelClass,
			IntrospectedTable introspectedTable) {
		addClientUpdateByPrimaryKeySelective(method);
		return true;
	}

	private void addClientUpdateByPrimaryKeySelective(Method method) {
		addGenericMethod(method, FullyQualifiedJavaType.getIntInstance(), genericModel);
	}

	@Override
	public boolean clientUpdateByPrimaryKeyWithBLOBsMethodGenerated(Method method, Interface interfaze,
			IntrospectedTable introspectedTable) {
		addClientUpdateByPrimaryKeyWithBLOBs(method);
		return true;
	}

	@Override
	public boolean clientUpdateByPrimaryKeyWithBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
			IntrospectedTable introspectedTable) {
		addClientUpdateByPrimaryKeyWithBLOBs(method);
		return true;
	}

	private void addClientUpdateByPrimaryKeyWithBLOBs(Method method) {
		addGenericMethod(method, FullyQualifiedJavaType.getIntInstance(), genericModel);
	}

	@Override
	public boolean clientUpdateByPrimaryKeyWithoutBLOBsMethodGenerated(Method method, Interface interfaze,
			IntrospectedTable introspectedTable) {
		addClientUpdateByPrimaryKeyWithoutBLOBs(method);
		return true;
	}

	@Override
	public boolean clientUpdateByPrimaryKeyWithoutBLOBsMethodGenerated(Method method, TopLevelClass topLevelClass,
			IntrospectedTable introspectedTable) {
		addClientUpdateByPrimaryKeyWithoutBLOBs(method);
		return true;
	}

	private void addClientUpdateByPrimaryKeyWithoutBLOBs(Method method) {
		addGenericMethod(method, FullyQualifiedJavaType.getIntInstance(), genericModel);
	}

	@Override
	public boolean clientInsertSelectiveMethodGenerated(Method method, Interface interfaze,
			IntrospectedTable introspectedTable) {
		addClientInsertSelective(method);
		return true;
	}

	@Override
	public boolean clientInsertSelectiveMethodGenerated(Method method, TopLevelClass topLevelClass,
			IntrospectedTable introspectedTable) {
		addClientInsertSelective(method);
		return true;
	}

	private void addClientInsertSelective(Method method) {
		addGenericMethod(method, FullyQualifiedJavaType.getIntInstance(), genericModel);
	}

	@Override
	public boolean clientSelectAllMethodGenerated(Method method, Interface interfaze,
			IntrospectedTable introspectedTable) {
		addClientSelectAll(method);
		return true;
	}

	@Override
	public boolean clientSelectAllMethodGenerated(Method method, TopLevelClass topLevelClass,
			IntrospectedTable introspectedTable) {
		addClientSelectAll(method);
		return true;
	}

	private void addClientSelectAll(Method method) {
		addGenericMethod(method, genericModel);
	}

}
