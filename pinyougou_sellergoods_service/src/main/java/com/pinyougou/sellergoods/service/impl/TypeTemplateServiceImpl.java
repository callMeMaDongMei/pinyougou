package com.pinyougou.sellergoods.service.impl;

import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.pinyougou.mapper.TbSpecificationOptionMapper;
import com.pinyougou.pojo.TbSpecificationOption;
import com.pinyougou.pojo.TbSpecificationOptionExample;
import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.TbTypeTemplateMapper;
import com.pinyougou.pojo.TbTypeTemplate;
import com.pinyougou.pojo.TbTypeTemplateExample;
import com.pinyougou.pojo.TbTypeTemplateExample.Criteria;
import com.pinyougou.sellergoods.service.TypeTemplateService;

import entity.PageResult;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 服务实现层
 *
 * @author Administrator
 */
@Service
public class TypeTemplateServiceImpl implements TypeTemplateService {

    @Autowired
    private TbTypeTemplateMapper typeTemplateMapper;

    /**
     * 查询全部
     */
    @Override
    public List<TbTypeTemplate> findAll() {
        return typeTemplateMapper.selectByExample(null);
    }

    /**
     * 按分页查询
     */
    @Override
    public PageResult findPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<TbTypeTemplate> page = (Page<TbTypeTemplate>) typeTemplateMapper.selectByExample(null);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 增加
     */
    @Override
    public void add(TbTypeTemplate typeTemplate) {
        typeTemplateMapper.insert(typeTemplate);
    }


    /**
     * 修改
     */
    @Override
    public void update(TbTypeTemplate typeTemplate) {
        typeTemplateMapper.updateByPrimaryKey(typeTemplate);
    }

    /**
     * 根据ID获取实体
     *
     * @param id
     * @return
     */
    @Override
    public TbTypeTemplate findOne(Long id) {
        return typeTemplateMapper.selectByPrimaryKey(id);
    }

    /**
     * 批量删除
     */
    @Override
    public void delete(Long[] ids) {
        for (Long id : ids) {
            typeTemplateMapper.deleteByPrimaryKey(id);
        }
    }


    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 缓存品牌列表和详细规格列表
     */
    private void saveToRedis() {
        List<TbTypeTemplate> templateList = findAll();
        for (TbTypeTemplate template : templateList) {

            List bandList = JSON.parseArray(template.getBrandIds(), Map.class);
            //将模板表数据存入缓存
            redisTemplate.boundHashOps("brandList").put(template.getId(), bandList);

            //得到规格列表,并存入缓存,此时缓存有itemCat,模板表的品牌信息(以模板id作为小key),以及规格信息
            List<Map> specList = findSpecList(template.getId());
            redisTemplate.boundHashOps("specList").put(template.getId(), specList);
        }
    }

    /**
     * 查询
     * @param typeTemplate
     * @param pageNum 当前页 码
     * @param pageSize 每页记录数
     * @return
     */
    @Override
    public PageResult findSth(TbTypeTemplate typeTemplate, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        TbTypeTemplateExample example = new TbTypeTemplateExample();
        Criteria criteria = example.createCriteria();

        if (typeTemplate != null) {
            if (typeTemplate.getName() != null && typeTemplate.getName().length() > 0) {
                criteria.andNameLike("%" + typeTemplate.getName() + "%");
            }
            if (typeTemplate.getSpecIds() != null && typeTemplate.getSpecIds().length() > 0) {
                criteria.andSpecIdsLike("%" + typeTemplate.getSpecIds() + "%");
            }
            if (typeTemplate.getBrandIds() != null && typeTemplate.getBrandIds().length() > 0) {
                criteria.andBrandIdsLike("%" + typeTemplate.getBrandIds() + "%");
            }
            if (typeTemplate.getCustomAttributeItems() != null && typeTemplate.getCustomAttributeItems().length() > 0) {
                criteria.andCustomAttributeItemsLike("%" + typeTemplate.getCustomAttributeItems() + "%");
            }

        }
        Page<TbTypeTemplate> page = (Page<TbTypeTemplate>) typeTemplateMapper.selectByExample(example);

        //存入缓存
        saveToRedis();
        System.out.println("缓存品牌列表和详细规格列表...");

        return new PageResult(page.getTotal(), page.getResult());
    }
    @Autowired
    private TbSpecificationOptionMapper specificationOptionMapper;

    //添加商品,之商品规格处理根据模板id查询specIds,规格集合list<map>
    @Override
    public List<Map> findSpecList(Long id) {
        //根据模板id查出模板实体,然后得到模板规格实体
        TbTypeTemplate typeTemplate = typeTemplateMapper.selectByPrimaryKey(id);
        String specIds = typeTemplate.getSpecIds();//结果类似这样,然后就是规格信息不完整,需要处理[{"id":27,"text":"网络"},{"id":32,"text":"机身内存"}]
        //先转为json的list集合
        List<Map> list = JSON.parseArray(specIds, Map.class);
        //遍历集合,添加specificationOption
        for (Map map : list) {
            //查出specificationOption,put进入map集合中
            TbSpecificationOptionExample example = new TbSpecificationOptionExample();
            TbSpecificationOptionExample.Criteria criteria = example.createCriteria();
            criteria.andSpecIdEqualTo(new Long((Integer) map.get("id")));
            List<TbSpecificationOption> options = specificationOptionMapper.selectByExample(example);
            map.put("options", options);
        }
        return list;
    }
}
