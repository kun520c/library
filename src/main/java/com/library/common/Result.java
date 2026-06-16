package com.library.common;

import com.library.model.vo.BookVO;
import com.library.model.vo.PageVO;
import lombok.Data;
import java.util.List;

@Data
public class Result {
    private Integer code;
    private String message;
    private Object data;

    public static Result success(Object data){
        Result r = new Result();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static Result success(){
        Result r = new Result();
        r.code = 200;
        r.message = "success";
        return r;
    }

    public static Result error(Integer code,String message){
        Result r = new Result();
        r.code = code;
        r.message = message;
        return r;
    }

    public static Result error(String message) {
        Result r = new Result();
        r.code = 500;
        r.message = message;
        return r;
    }

    public static Result page(List<BookVO> list,long total){
        Result r = new Result();
        PageVO pageVO = new PageVO(list,total);
        r.code = 200;
        r.message = "success";
        r.data = pageVO;
        return r;
    }
}
