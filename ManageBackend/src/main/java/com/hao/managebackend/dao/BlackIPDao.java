package com.hao.managebackend.Dao;

import com.hao.managebackend.Model.BlackIP;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface BlackIPDao {

    @Insert("insert into black_ip(ip, createTime) values(#{ip}, #{createTime})")
    int save(BlackIP blackIP);

    @Delete("delete from black_ip where ip = #{ip}")
    int delete(String ip);

    @Select("select * from black_ip t where t.ip = #{ip}")
    BlackIP findByIp(String ip);

    int count(Map<String, Object> params);

    List<BlackIP> findData(Map<String, Object> params);
}