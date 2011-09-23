/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.console.rebind.forms;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.rebind.ClassSourceFileComposerFactory;
import com.google.gwt.user.rebind.SourceWriter;
import org.jboss.as.console.client.widgets.forms.Address;
import org.jboss.as.console.client.widgets.forms.Binding;
import org.jboss.ballroom.client.util.StringTokenizer;
import org.jboss.dmr.client.ModelNode;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.jboss.dmr.client.ModelDescriptionConstants.ADDRESS;

/**
 * @author Heiko Braun
 * @date 4/19/11
 */
public class PropertyMetaDataGenerator extends Generator{

    private static final String BEAN_FACTORY_NAME = "org.jboss.as.console.client.shared.BeanFactory";
    /**
     * Simple name of class to be generated
     */
    private String className = null;

    /**
     * Package name of class to be generated
     */
    private String packageName = null;

    /**
     * Fully qualified class name passed into GWT.create()
     */
    private String typeName = null;


    public String generate(TreeLogger logger, GeneratorContext context, String typeName)
            throws UnableToCompleteException
    {
        this.typeName = typeName;
        TypeOracle typeOracle = context.getTypeOracle();

        try
        {
            // get classType and save instance variables
            JClassType classType = typeOracle.getType(typeName);
            packageName = classType.getPackage().getName();
            className = classType.getSimpleSourceName() + "Impl";

            // Generate class source code
            generateClass(logger, context);

        }
        catch (Exception e)
        {
            // record to logger that Map generation threw an exception
            e.printStackTrace(System.out);
            logger.log(TreeLogger.ERROR, "Failed to generate property meta data", e);
        }

        // return the fully qualified name of the class generated
        return packageName + "." + className;
    }

    /**
     * Generate source code for new class. Class extends
     * <code>HashMap</code>.
     *
     * @param logger  Logger object
     * @param context Generator context
     */
    private void generateClass(TreeLogger logger, GeneratorContext context)
    {

        // get print writer that receives the source code
        PrintWriter printWriter = context.tryCreate(logger, packageName, className);

        // print writer if null, source code has ALREADY been generated, return
        if (printWriter == null) return;

        // init composer, set class properties, create source writer
        ClassSourceFileComposerFactory composerFactory =
                new ClassSourceFileComposerFactory(packageName, className);

        // Imports
        composerFactory.addImport("org.jboss.as.console.client.widgets.forms.*");
        composerFactory.addImport("java.util.*");

        // Interfaces
        composerFactory.addImplementedInterface("org.jboss.as.console.client.widgets.forms.PropertyMetaData");

        // SourceWriter
        SourceWriter sourceWriter = composerFactory.createSourceWriter(context, printWriter);

        // fields
        generateFields(sourceWriter);

        // ctor
        generateConstructor(logger, context, sourceWriter);

        // Methods
        generateMethods(sourceWriter);

        // close generated class
        sourceWriter.outdent();
        sourceWriter.println("}");

        // commit generated class
        context.commit(logger, printWriter);
    }

    private void generateFields(SourceWriter sourceWriter) {
        sourceWriter.println("private Map<Class<?>, List<PropertyBinding>> registry = new HashMap<Class<?>,List<PropertyBinding>>();");
        sourceWriter.println("private Map<Class<?>, AddressBinding> addressing= new HashMap<Class<?>, AddressBinding>();");
        sourceWriter.println("private Map<Class<?>, Mutator> mutators = new HashMap<Class<?>, Mutator>();");
    }

    private void generateConstructor(TreeLogger logger, GeneratorContext context , SourceWriter sourceWriter)
    {
        // start constructor source generation
        sourceWriter.println("public " + className + "() { ");
        sourceWriter.indent();
        sourceWriter.println("super();");

        try {
            Class<?> beanFactoryClass = getClass().getClassLoader().loadClass(BEAN_FACTORY_NAME);

            int idx = 0;

            for(Method method : beanFactoryClass.getDeclaredMethods())
            {
                Type returnType = method.getGenericReturnType();
                if(returnType instanceof ParameterizedType){
                    ParameterizedType type = (ParameterizedType) returnType;
                    Type[] typeArguments = type.getActualTypeArguments();

                    if(typeArguments[0] instanceof Class)
                    {
                        Class beanTypeClass = (Class) typeArguments[0];
                        sourceWriter.println("registry.put("+beanTypeClass.getName()+".class, new ArrayList<PropertyBinding>());");


                        // --------------------------------
                        // Mutator

                        sourceWriter.println("Mutator mut_"+idx+" = new Mutator<"+beanTypeClass.getName()+">();");
                        sourceWriter.println("mutators.put("+beanTypeClass.getName()+".class , mut_"+idx+");");

                        // -----------------------------
                        // PropertyBinding

                        List<BindingDeclaration> bindings = mapProperties(beanTypeClass);

                        for(BindingDeclaration decl : bindings)
                        {
                            if(decl.isIgnore()) continue;

                            sourceWriter.println("registry.get("+beanTypeClass.getName()+".class).add(");
                            sourceWriter.indent();
                            sourceWriter.println("new PropertyBinding(\""+decl.getJavaName()+"\", \""+decl.getDetypedName()+"\")");
                            sourceWriter.outdent();
                            sourceWriter.println(");");


                            // create and register setters
                            sourceWriter.println("mut_"+idx+".register(\"" + decl.getJavaName() + "\", new Setter<"+beanTypeClass.getName()+">() {\n" +
                                        "public void invoke("+decl.getBeanClassName()+" entity, Object value) {\n" +
                                            "entity.set"+decl.getPropertyName()+"(("+decl.getJavaTypeName()+")value);\n"+
                                        "}\n"+
                                    "});\n");

                        }

                        // -----------------------------
                        // AddressBinding

                        AddressDeclaration addr = parseAddress(beanTypeClass);

                        sourceWriter.println("AddressBinding addr_"+idx+" = new AddressBinding();");
                        sourceWriter.println("addressing.put("+beanTypeClass.getName()+".class , addr_"+idx+");");

                        for(String[] token : addr.getAddress()) {
                            sourceWriter.println("addr_"+idx+".add(\""+token[0]+"\", \""+token[1]+"\");");
                        }

                        sourceWriter.println("");
                        sourceWriter.println("");
                        sourceWriter.println("// ---- End " +beanTypeClass.getName() +" ----");
                    }
                }

                idx++;

            }

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load " + BEAN_FACTORY_NAME);
        }

        sourceWriter.outdent();
        sourceWriter.println("}");
    }

    public static List<BindingDeclaration> mapProperties(Class beanTypeClass) {

        List<BindingDeclaration> bindings = new ArrayList<BindingDeclaration>();

        for(Method method : beanTypeClass.getMethods())
        {
            String methodName = method.getName();
            String token = null;

            if(methodName.startsWith("get"))
            {
                token = methodName.substring(3, methodName.length());
            }
            else if(methodName.startsWith("is"))
            {
                token = methodName.substring(2, methodName.length());
            }

            if(token!=null)
            {
                BindingDeclaration bindingDeclaration = createBindingDeclaration(beanTypeClass, method, token);
                if(bindingDeclaration!=null)
                    bindings.add(bindingDeclaration);
            }

        }

        return bindings;
    }

    public static AddressDeclaration parseAddress(Class beanTypeClass) {


        Address annotation = (Address)beanTypeClass.getAnnotation(Address.class);

        List<String[]> address = Collections.EMPTY_LIST;

        if(annotation!=null) // TODO: mandatory at some point
        {
            address = parseAddressString(annotation.value());
        }

        return new AddressDeclaration(address);
    }

    private static List<String[]> parseAddressString(String value) {
        List<String[]> address = new LinkedList<String[]>();

        if(value.equals("/")) // default parent value
            return address;

        java.util.StringTokenizer tok = new java.util.StringTokenizer(value, "/");
        while(tok.hasMoreTokens())
        {
            String nextToken = tok.nextToken();
            address.add(nextToken.split("="));
        }
        return address;
    }

    private static BindingDeclaration createBindingDeclaration(Class beanTypeClass, Method method, String token) {


        String firstLetter = token.substring(0,1);
        String remainder   = token.substring(1);
        String normalized = firstLetter.toLowerCase() + remainder;

        String javaName = normalized;
        String detypedName = javaName;

        // @Binding can override the detyped name
        Binding bindingDeclaration = method.getAnnotation(Binding.class);
        boolean ignore = false;

        if(bindingDeclaration!=null)
        {
            detypedName = bindingDeclaration.detypedName();
            ignore = bindingDeclaration.ignore();
        }

        BindingDeclaration decl = new BindingDeclaration(detypedName, javaName, ignore, beanTypeClass.getName());
        decl.setJavaTypeName(method.getReturnType().getName());
        return decl;
    }

    private void generateMethods(SourceWriter sourceWriter)
    {

        sourceWriter.println("public List<PropertyBinding> getBindingsForType(Class<?> type) { ");
        sourceWriter.indent();
        sourceWriter.println("return registry.get(type);");
        sourceWriter.outdent();
        sourceWriter.println("}");

        sourceWriter.println("public BeanMetaData getBeanMetaData(Class<?> type) { ");
        sourceWriter.indent();
        sourceWriter.println("return new BeanMetaData(type, addressing.get(type), registry.get(type));");
        sourceWriter.outdent();
        sourceWriter.println("}");


        sourceWriter.println("public Mutator getMutator(Class<?> type) { ");
        sourceWriter.indent();
        sourceWriter.println("return null;");
        sourceWriter.outdent();
        sourceWriter.println("}");
    }

}
