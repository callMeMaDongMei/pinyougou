package com.pinyougou.sellergoods.service;
import java.util.List;
import java.util.Map;

import com.pinyougou.pojo.TbSpecification;

import com.pinyougou.pojogroup.Specification;
import entity.PageResult;
/**
 * 服务层接口
 * @author Administrator
 *
 */
public interface SpecificationService {

	/**
	 * 返回全部列表
	 * @return
	 */
	public List<TbSpecification> findAll();
	
	
	/**
	 * 返回分页列表
	 * @return
	 */
	public PageResult findPage(int pageNum, int pageSize);
	
	
	/**
	 * 增加
	*/
	//改造自动生成的添加方法(接受自己创建的组合实体类)
	//public void add(TbSpecification specification);
	public void add(Specification specification);

	
	/**
	 * 修改
	 */
	public void update(Specification specification);
	

	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	public Specification findOne(Long id);
	
	
	/**
	 * 批量删除
	 * @param ids
	 */
	public void delete(Long[] ids);

	/**
	 * 分页
	 * @param pageNum 当前页 码
	 * @param pageSize 每页记录数
	 * @return
	 */
	public PageResult findSth(TbSpecification specification, int pageNum, int pageSize);

	public List<Map> selectOptionList();

	
}
