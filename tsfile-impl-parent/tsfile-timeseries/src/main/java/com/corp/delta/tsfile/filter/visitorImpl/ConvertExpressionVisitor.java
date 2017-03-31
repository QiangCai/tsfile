package com.corp.delta.tsfile.filter.visitorImpl;

import com.corp.delta.tsfile.filter.definition.FilterExpression;
import com.corp.delta.tsfile.filter.definition.FilterFactory;
import com.corp.delta.tsfile.filter.definition.SingleSeriesFilterExpression;
import com.corp.delta.tsfile.filter.definition.operators.And;
import com.corp.delta.tsfile.filter.definition.operators.Eq;
import com.corp.delta.tsfile.filter.definition.operators.GtEq;
import com.corp.delta.tsfile.filter.definition.operators.LtEq;
import com.corp.delta.tsfile.filter.definition.operators.Not;
import com.corp.delta.tsfile.filter.definition.operators.NotEq;
import com.corp.delta.tsfile.filter.definition.operators.Or;

/**
 * To remove not operators, convert all operators recursively. 
 * Not(and(eq(), not(eq(y))) -> Or(notEq(), eq(y)).
 * 
 * @author CGF
 */
public class ConvertExpressionVisitor implements FilterVisitor<FilterExpression> {

    private InvertExpressionVisitor invertor = new InvertExpressionVisitor();

    public FilterExpression convert(FilterExpression exp) {
        return exp.accept(this);
    }

    @Override
    public <T extends Comparable<T>> FilterExpression visit(Eq<T> eq) {
        return eq;
    }

    @Override
    public <T extends Comparable<T>> FilterExpression visit(NotEq<T> notEq) {
        return notEq;
    }

    @Override
    public <T extends Comparable<T>> FilterExpression visit(LtEq<T> ltEq) {
        return ltEq;
    }

    @Override
    public <T extends Comparable<T>> FilterExpression visit(GtEq<T> gtEq) {
        return gtEq;
    }


    @Override
    public FilterExpression visit(And and) {
        return FilterFactory.and((and.getLeft()), convert(and.getRight()));
    }

    @Override
    public FilterExpression visit(Or or) {
        return FilterFactory.or(convert(or.getLeft()), convert(or.getRight()));
    }

    @Override
    public FilterExpression visit(Not not) {
        return invertor.invert(not.getFilterExpression());
    }
}

