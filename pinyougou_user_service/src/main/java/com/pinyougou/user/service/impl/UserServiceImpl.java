package com.pinyougou.user.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSON;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.TbUserMapper;
import com.pinyougou.pojo.TbUser;
import com.pinyougou.pojo.TbUserExample;
import com.pinyougou.pojo.TbUserExample.Criteria;
import com.pinyougou.user.service.UserService;

import entity.PageResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import javax.jms.*;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private TbUserMapper userMapper;

    /**
     * 查询全部
     */
    @Override
    public List<TbUser> findAll() {
        return userMapper.selectByExample(null);
    }

    /**
     * 按分页查询
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<TbUser> page = (Page<TbUser>) userMapper.selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 增加
     */
    @Override
    public void add(TbUser user) {
        user.setCreated(new Date());
        user.setUpdated(new Date());
        String password = DigestUtils.md5Hex(user.getPassword());
        user.setPassword(password);//MD5加密
        user.setSourceType("1");//浏览器类型
        userMapper.insert(user);
    }


    /**
     * 修改
     */
    @Override
    public void update(TbUser user) {
        userMapper.updateByPrimaryKey(user);
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public TbUser findOne(Long id) {
        return userMapper.selectByPrimaryKey(id);
    }

    /**
     * 批量删除
     */
    @Override
    public void delete(Long[] ids) {
        for (Long id : ids) {
            userMapper.deleteByPrimaryKey(id);
        }
    }


    @Override
    public PageResult findPage(TbUser user, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        TbUserExample example = new TbUserExample();
        Criteria criteria = example.createCriteria();

        if (user != null) {
            if (user.getUsername() != null && user.getUsername().length() > 0) {
                criteria.andUsernameLike("%" + user.getUsername() + "%");
            }
            if (user.getPassword() != null && user.getPassword().length() > 0) {
                criteria.andPasswordLike("%" + user.getPassword() + "%");
            }
            if (user.getPhone() != null && user.getPhone().length() > 0) {
                criteria.andPhoneLike("%" + user.getPhone() + "%");
            }
            if (user.getEmail() != null && user.getEmail().length() > 0) {
                criteria.andEmailLike("%" + user.getEmail() + "%");
            }
            if (user.getSourceType() != null && user.getSourceType().length() > 0) {
                criteria.andSourceTypeLike("%" + user.getSourceType() + "%");
            }
            if (user.getNickName() != null && user.getNickName().length() > 0) {
                criteria.andNickNameLike("%" + user.getNickName() + "%");
            }
            if (user.getName() != null && user.getName().length() > 0) {
                criteria.andNameLike("%" + user.getName() + "%");
            }
            if (user.getStatus() != null && user.getStatus().length() > 0) {
                criteria.andStatusLike("%" + user.getStatus() + "%");
            }
            if (user.getHeadPic() != null && user.getHeadPic().length() > 0) {
                criteria.andHeadPicLike("%" + user.getHeadPic() + "%");
            }
            if (user.getQq() != null && user.getQq().length() > 0) {
                criteria.andQqLike("%" + user.getQq() + "%");
            }
            if (user.getIsMobileCheck() != null && user.getIsMobileCheck().length() > 0) {
                criteria.andIsMobileCheckLike("%" + user.getIsMobileCheck() + "%");
            }
            if (user.getIsEmailCheck() != null && user.getIsEmailCheck().length() > 0) {
                criteria.andIsEmailCheckLike("%" + user.getIsEmailCheck() + "%");
            }
            if (user.getSex() != null && user.getSex().length() > 0) {
                criteria.andSexLike("%" + user.getSex() + "%");
            }

        }

        Page<TbUser> page = (Page<TbUser>) userMapper.selectByExample(example);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private Destination smsDestination;
    @Autowired
    private JmsTemplate jmsTemplate;
    @Value("${template_code}")
    private String template_code;
    @Value("${sign_name}")
    private String sign_name;

    /**
     * 验证码思路
     * 存进缓存,并且设置存活时间
     *
     * @param phone
     */
    @Override
    public void createSmsCode(final String phone) {
        //生成6为随机数
        final String code = RandomStringUtils.randomNumeric(6);
        System.out.println("code:" + code);
        redisTemplate.opsForValue().set(phone, code, 60, TimeUnit.SECONDS);
        //String sys = (String) redisTemplate.opsForValue().get(phone);
        //System.out.println("sys:"+sys);
        //微服务发送短信目标注册号码和验证码模板短信
        jmsTemplate.send(smsDestination, new MessageCreator() {
            @Override
            public Message createMessage(Session session) throws JMSException {
                MapMessage message = session.createMapMessage();
                message.setString("mobile", phone);//手机号
                message.setString("template_code", template_code);//消息模板号
                message.setString("sign_name", sign_name);//签名
                Map map = new HashMap();
                map.put("code", code);
                message.setString("param", JSON.toJSONString(map));//签名
                return message;
            }
        });
    }

    /**
     * 校验验证码
     *
     * @param phone
     * @param smscode
     * @return
     */
    @Override
    public boolean checkSmsCode(String phone, String smscode) {
        //从缓存中获取系统生成的验证码
        String syscode = (String) redisTemplate.opsForValue().get(phone);
        System.out.println("缓存中正确验证码:" + syscode);
        System.out.println("用户输入的:" + smscode);
        if(syscode==null){
            return false;
        }
        if(!syscode.equals(smscode)){
                return false;
        }
        return true;
    }
}
