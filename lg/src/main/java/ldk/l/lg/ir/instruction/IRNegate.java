package ldk.l.lg.ir.instruction;

import ldk.l.lg.ir.IRVisitor;
import ldk.l.lg.ir.operand.IROperand;
import ldk.l.lg.ir.operand.IRVirtualRegister;
import ldk.l.lg.ir.type.IRType;

public final class IRNegate extends IRInstruction {
    public boolean isAtomic;
    public IRType type;
    public IROperand operand;
    public IRVirtualRegister target;

    public IRNegate(boolean isAtomic, IRType type, IROperand operand, IRVirtualRegister target) {
        this.isAtomic = isAtomic;
        this.type = type;
        this.operand = operand;
        this.target = target;
    }

    @Override
    public Object accept(IRVisitor visitor, Object additional) {
        return visitor.visitNegate(this, additional);
    }

    @Override
    public String toString() {
        return target + " = " + (isAtomic ? "atomic_" : "") + "negate " + type + operand;
    }
}
