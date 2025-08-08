package ldk.l.lc.ast.file;

import ldk.l.lc.ast.LCAstUtil;
import ldk.l.lc.ast.LCAstVisitor;
import ldk.l.lc.ast.base.LCBlock;
import ldk.l.lc.ast.base.LCStatement;
import ldk.l.lc.ast.statement.declaration.object.LCObjectDeclaration;
import ldk.l.lc.ast.statement.LCImport;
import ldk.l.lc.util.Position;
import ldk.l.lc.util.scope.Scope;

import java.util.ArrayList;
import java.util.List;

public final class LCSourceCodeFile extends LCSourceFile {
    public String packageName = null;
    public Scope scope = null;
    public LCBlock body;
    public List<LCSourceFileProxy> proxies = new ArrayList<>();


    public LCSourceCodeFile(String filename, LCBlock body, Position pos, boolean isErrorNode) {
        super(filename, pos, isErrorNode);
        this.body = body;
        this.body.parentNode = this;
    }

    @Override
    public Object accept(LCAstVisitor visitor, Object additional) {
        return visitor.visitSourceCodeFile(this, additional);
    }

    @Override
    public String toString() {
        return "LCSourceCodeFile{" +
                "packageName='" + packageName + '\'' +
                ", body=" + body +
                ", proxies=" + proxies +
                ", filepath='" + filepath + '\'' +
                ", isErrorNode=" + isErrorNode +
                ", position=" + position +
                '}';
    }

    @Override
    public LCObjectDeclaration[] getObjectDeclarations() {
        ArrayList<LCObjectDeclaration> objectDeclarations = new ArrayList<>();
        for (LCStatement LCStatement : this.body.statements) {
            if (LCStatement instanceof LCObjectDeclaration objectDeclaration) {
                objectDeclarations.add(objectDeclaration);
                objectDeclarations.addAll(List.of(LCAstUtil.getObjectDeclarations(objectDeclaration)));
            }
        }
        return objectDeclarations.toArray(new LCObjectDeclaration[0]);
    }

    @Override
    public LCObjectDeclaration getObjectDeclaration(String fullName) {
        for (LCObjectDeclaration objectDecl : this.getObjectDeclarations()) {
            if (objectDecl.getFullName().equals(fullName)) return objectDecl;
        }
        return null;
    }

    public LCObjectDeclaration getObjectDeclarationByName(String name) {
        for (LCStatement LCStatement : this.body.statements) {
            if (LCStatement instanceof LCObjectDeclaration objectDecl && objectDecl.name.equals(name))
                return objectDecl;
        }
        return null;
    }

    public LCImport[] getImportStatements() {
        ArrayList<LCImport> lcImports = new ArrayList<>();
        for (LCStatement LCStatement : this.body.statements) {
            if (LCStatement instanceof LCImport lcImport) lcImports.add(lcImport);
        }
        return lcImports.toArray(new LCImport[0]);
    }
}
