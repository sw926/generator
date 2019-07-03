/**
 *    Copyright 2006-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.generator.internal;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedHashSet;
import java.util.Set;

public class JavaFileMergerJaxp {

    public String getNewJavaFile(String newFileSource, String existingFileFullPath) throws FileNotFoundException {
        CompilationUnit newCompilationUnit = JavaParser.parse(newFileSource);
        CompilationUnit existingCompilationUnit = JavaParser.parse(new File(existingFileFullPath));
        return mergerFile(newCompilationUnit, existingCompilationUnit);
    }

    public String mergerFile(CompilationUnit newCompilationUnit, CompilationUnit existingCompilationUnit) {
        CompilationUnit outputUnit = new CompilationUnit();

        newCompilationUnit.getPackageDeclaration().ifPresent(outputUnit::setPackageDeclaration);

        //合并imports
        Set<ImportDeclaration> importSet = new LinkedHashSet<>();
        importSet.addAll(existingCompilationUnit.getImports());
        importSet.addAll(newCompilationUnit.getImports());
        importSet.forEach(outputUnit::addImport);

        for (TypeDeclaration<?> oldType : existingCompilationUnit.getTypes()) {
            TypeDeclaration<?> newType = newCompilationUnit.getTypes().stream().filter(typeDeclaration -> typeDeclaration.getName().equals(oldType.getName())).findFirst().orElse(null);
            if (newType != null) {
                TypeDeclaration<?> outputType = newType.clone();

                // 清理所有的 members
                outputType.setMembers(new NodeList<>());

                // fields
                Set<BodyDeclaration<?>> newMembers = new LinkedHashSet<>(newType.getMembers());
                Set<BodyDeclaration<?>> oldMember = new LinkedHashSet<>(oldType.getMembers());
                oldMember.removeIf(declaration -> declaration.getComment().isPresent() && declaration.getComment().get().getContent().contains("@mbg.generated"));
                newMembers.addAll(oldMember);
                newMembers.forEach(outputType::addMember);

                outputUnit.addType(outputType);
            }
        }

        return outputUnit.toString();
    }

}