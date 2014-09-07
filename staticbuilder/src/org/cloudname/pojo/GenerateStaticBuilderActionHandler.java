package org.cloudname.pojo;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiStatement;

public class GenerateStaticBuilderActionHandler extends EditorWriteActionHandler {

    @Override
    public void executeWriteAction(final Editor editor, final DataContext dataContext) {
        IdeaUtil util = ApplicationManager.getApplication().getComponent(IdeaUtil.class);
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(editor.getProject());
        PsiElementFactory psiElementFactory = psiFacade.getElementFactory();
        PsiClass clazz = util.getCurrentClass(editor);
        addBeanConstructors(clazz, psiElementFactory);
        changeFieldsToFinal(clazz, psiElementFactory);
        addGetters(clazz, psiElementFactory);
        PsiClass builder = getBuilderClass(clazz, psiElementFactory);
        PsiStatement statement = psiElementFactory.createStatementFromText(
            builder.getText().replace("class", "static class"), clazz);
        clazz.addBefore(statement, clazz.getRBrace());
    }

    protected PsiClass getBuilderClass(PsiClass currentClazz, PsiElementFactory psiElementFactory) {
        PsiClass builder = psiElementFactory.createClass("Builder");
        for (PsiField field : currentClazz.getFields()) {
            String fieldType = field.getType().getCanonicalText();
            String fieldName = field.getName();
            builder.add(psiElementFactory.createFieldFromText(getFieldText(fieldName, fieldType), builder));
            builder.add(psiElementFactory.createMethodFromText(getSetterText(fieldName, fieldType,
                builder.getName()), builder));
        }
        builder.add(psiElementFactory.createMethodFromText(getBuildText(currentClazz), builder));
        System.out.println(builder.getText());

        return builder;
    }

    protected String getFieldText(String fieldName, String fieldType) {
        return new StringBuffer()
            .append("private ").append(fieldType)
            .append(" ").append(fieldName).append(";")
            .toString();
    }

    protected String getSetterText(String fieldName, String fieldType, String builderName) {
        return new StringBuffer()
            .append("@JsonSetter(\"").append(fieldName).append("\")")
            .append("public ").append(builderName)
            .append(" ")
            .append(fieldName).append("(").append(fieldType).append(" ").append(fieldName).append("){\n")
            .append("this.").append(fieldName).append("=").append(fieldName).append(";").append("return this;\n}\n")
            .toString();
    }

    protected String getGetterText(String fieldName, String fieldType) {
        return new StringBuffer()
            .append("@JsonGetter(\"").append(fieldName).append("\")")
            .append("public ").append(fieldType)
            .append(" get").append(fieldName.substring(0,1).toUpperCase()).append(fieldName.substring(1))
            .append("(){\n")
            .append("return this.").append(fieldName).append(";").append("\n}\n")
            .toString();
    }

    protected String getBuildText(PsiClass returnClass) {
        String returnType = returnClass.getName();
        StringBuffer returnBuffer = new StringBuffer()
            .append("public ").append(returnType).append(" build(){\n")
            .append(returnType).append(" instance = new ").append(returnType).append("(");
        PsiField[] fields = returnClass.getFields();
        for (int i=0;i<fields.length;i++) {
            String field = fields[i].getName();
            returnBuffer.append(field);
            if (i<fields.length-1) returnBuffer.append(", ");
        }
        returnBuffer.append(");\n")
                    .append("//TODO Ensure all required fields are set\n")
                    .append("return instance;\n")
                    .append("}\n");
        return returnBuffer.toString();
    }

    protected void addBeanConstructors(PsiClass clazz, PsiElementFactory psiElementFactory) {
        StringBuffer emptyConstructor = new StringBuffer()
            .append("private ").append(clazz.getName()).append("(){\n")
            .append("throw new IllegalStateException();\n")
            .append("}");
        clazz.add(psiElementFactory.createMethodFromText(emptyConstructor.toString(), clazz));
        StringBuffer mainConstructor = new StringBuffer()
            .append("private ").append(clazz.getName()).append("(");
        PsiField[] fields = clazz.getFields();
        for (int i=0;i<fields.length;i++) {
            PsiField field = fields[i];
            mainConstructor.append(field.getType().getCanonicalText()).append(" ").append(field.getName());
            if (i<fields.length-1) mainConstructor.append(", ");
        }
        mainConstructor.append(") {\n");
        for (int i=0;i<fields.length;i++) {
            PsiField field = fields[i];
            mainConstructor.append("this.").append(field.getName()).append(" = ").append(field.getName())
                .append(";\n");
        }
        mainConstructor.append("}\n");
        clazz.add(psiElementFactory.createMethodFromText(mainConstructor.toString(), clazz));
    }

    protected void changeFieldsToFinal(PsiClass clazz, PsiElementFactory psiElementFactory) {
        PsiField[] fields = clazz.getFields();
        for (int i=0;i<fields.length;i++) {
            PsiField field = fields[i];
            String fieldName = field.getName();
            String fieldType = field.getType().getCanonicalText();
            field.delete();
            StringBuffer finalField = new StringBuffer()
                .append("private final ").append(fieldType).append(" ").append(fieldName).append(";");
            clazz.add(psiElementFactory.createFieldFromText(finalField.toString(), clazz));
        }
    }

    protected void addGetters(PsiClass clazz, PsiElementFactory psiElementFactory) {
        PsiField[] fields = clazz.getFields();
        for (int i=0;i<fields.length;i++) {
            PsiField field = fields[i];
            String fieldName = field.getName();
            String fieldType = field.getType().getCanonicalText();
            clazz.add(psiElementFactory.createMethodFromText(getGetterText(fieldName, fieldType), clazz));
        }

    }
}
