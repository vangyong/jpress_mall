package io.jpress.ui.freemarker.function;

import io.jpress.core.render.freemarker.JFunction;
import io.jpress.model.query.ContentSpecItemQuery;

import java.math.BigInteger;

/**
 * @author heguoliang
 * @Description: TODO()
 * @date 2017-6-16 10:43
 */
public class ContentSpecValueChecked extends JFunction {

    @Override
    public Object onExec() {
        BigInteger content_id = getToBigInteger(0);
        BigInteger spec_id = getToBigInteger(1);
        BigInteger spec_value_id = getToBigInteger(2);
        if (content_id==null){
            return "";
        }
        if (spec_id==null){
            return "";
        }
        if (spec_value_id==null){
            return "";
        }
        long count= ContentSpecItemQuery.me().findcount(content_id, spec_id, spec_value_id);
        if(count > 0){
            return "checked=\"checked\"";
        }else{
            return "";
        }
    }
    
}
