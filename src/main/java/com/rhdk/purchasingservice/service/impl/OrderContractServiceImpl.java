package com.rhdk.purchasingservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.igen.acc.domain.dto.OrgUserDto;
import com.rhdk.purchasingservice.common.enums.ResultEnum;
import com.rhdk.purchasingservice.common.utils.BeanCopyUtil;
import com.rhdk.purchasingservice.common.utils.NumberUtils;
import com.rhdk.purchasingservice.common.utils.ResultVOUtil;
import com.rhdk.purchasingservice.common.utils.TokenUtil;
import com.rhdk.purchasingservice.common.utils.response.ResponseEnvelope;
import com.rhdk.purchasingservice.feign.AssetServiceFeign;
import com.rhdk.purchasingservice.mapper.OrderAttachmentMapper;
import com.rhdk.purchasingservice.mapper.OrderContractMapper;
import com.rhdk.purchasingservice.mapper.PurcasingContractMapper;
import com.rhdk.purchasingservice.pojo.dto.OrderAttachmentDTO;
import com.rhdk.purchasingservice.pojo.dto.OrderContractDTO;
import com.rhdk.purchasingservice.pojo.entity.OrderAttachment;
import com.rhdk.purchasingservice.pojo.entity.OrderContract;
import com.rhdk.purchasingservice.pojo.entity.PurcasingContract;
import com.rhdk.purchasingservice.pojo.query.OrderContractQuery;
import com.rhdk.purchasingservice.pojo.vo.OrderContractVO;
import com.rhdk.purchasingservice.service.CommonService;
import com.rhdk.purchasingservice.service.IOrderContractService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 合同表 服务实现类
 *
 * @author LMYOU
 * @since 2020-05-08
 */
@Slf4j
@Transactional
@Service
public class OrderContractServiceImpl extends ServiceImpl<OrderContractMapper, OrderContract>
    implements IOrderContractService {

  @Autowired private OrderContractMapper orderContractMapper;

  @Autowired private CommonService commonService;

  @Autowired private OrderAttachmentMapper orderAttachmentMapper;

  @Autowired private PurcasingContractMapper purcasingContractMapper;

  @Autowired private AssetServiceFeign assetServiceFeign;

  private static org.slf4j.Logger logger = LoggerFactory.getLogger(OrderContractServiceImpl.class);

  @Override
  public ResponseEnvelope searchOrderContractListPage(OrderContractQuery dto) {
    Page<OrderContractVO> page2 = new Page<OrderContractVO>();
    Page<OrderContract> page = new Page<OrderContract>();
    page.setSize(dto.getPageSize());
    page.setCurrent(dto.getCurrentPage());
    QueryWrapper<OrderContract> queryWrapper = new QueryWrapper<OrderContract>();
    OrderContract entity = new OrderContract();
    queryWrapper.orderByDesc("CREATE_DATE");
    logger.info("searchOrderContractListPage-获取合同id列表开始");
    List<Long> paramStr = orderContractMapper.getContractIdList(dto.getContractCompany());
    logger.info("searchOrderContractListPage-获取合同id列表结束，获取了" + paramStr.size() + "条");
    if (paramStr.size() > 0) {
      queryWrapper.in("ID", paramStr);
    } else {
      return ResultVOUtil.returnSuccess(page2);
    }
    dto.setContractCompany(null);
    // 这里加入模糊搜索条件
    if (!StringUtils.isEmpty(dto.getContractCode())) {
      queryWrapper.like("CONTRACT_CODE", dto.getContractCode());
      dto.setContractCode(null);
    }
    if (!StringUtils.isEmpty(dto.getContractName())) {
      queryWrapper.like("CONTRACT_NAME", dto.getContractName());
      dto.setContractName(null);
    }
    BeanCopyUtil.copyPropertiesIgnoreNull(dto, entity);
    entity.setOrgId(TokenUtil.getUserInfo().getOrganizationId());
    queryWrapper.setEntity(entity);
    page = orderContractMapper.selectPage(page, queryWrapper);
    List<OrderContract> resultList = page.getRecords();
    logger.info("getFileList-获取合同附件列表开始");
    List<OrderContractVO> contractVOList =
        resultList.stream()
            .map(
                a -> {
                  // 根据合同id去附件表里获取每个合同对应的附件
                  OrgUserDto userDto = commonService.getOrgUserById(a.getOrgId(), a.getCreateBy());
                  OrderContractVO mo = orderContractMapper.selectContractByCId(a.getId());
                  OrderAttachmentDTO attachmentDTO = new OrderAttachmentDTO();
                  attachmentDTO.setParentId(mo.getOrderId());
                  attachmentDTO.setAtttype(1);
                  OrderContractVO at =
                      OrderContractVO.builder()
                          .attachmentList(
                              assetServiceFeign
                                  .selectListByParentId(attachmentDTO, TokenUtil.getToken())
                                  .getData())
                          .contractCode(a.getContractCode())
                          .contractCompany(mo.getContractCompany())
                          .contractName(a.getContractName())
                          .contractDate(a.getContractDate())
                          .contractMoney(a.getContractMoney())
                          .id(mo.getOrderId())
                          .contractType(a.getContractType())
                          .createBy(a.getCreateBy())
                          .createDate(a.getCreateDate())
                          .updateBy(a.getUpdateBy())
                          .updateDate(a.getUpdateDate())
                          .delFlag(a.getDelFlag())
                          .createName(userDto.getUserInfo().getName())
                          .deptName(userDto.getGroupName())
                          .build();
                  return at;
                })
            .collect(Collectors.toList());
    logger.info("getFileList-获取合同附件列表结束");
    page2.setRecords(contractVOList); /**/
    page2.setSize(page.getSize());
    page2.setCurrent(page.getCurrent());
    page2.setTotal(page.getTotal());
    page2.setOrders(page.getOrders());
    return ResultVOUtil.returnSuccess(page2);
  }

  @Override
  public ResponseEnvelope searchOrderContractOne(Long id) {
    OrderContractVO orderContractVO = new OrderContractVO();
    OrderAttachmentDTO orderDto = new OrderAttachmentDTO();
    orderDto.setParentId(id);
    orderDto.setAtttype(1);
    List<Map<String, Object>> attachmentList =
        assetServiceFeign.selectListByParentId(orderDto, TokenUtil.getToken()).getData();
    logger.info("searchOrderContractOne-获取采购合同信息开始");
    PurcasingContract model = purcasingContractMapper.selectById(id);
    logger.info("searchOrderContractOne-获取采购合同信息：" + model.toString() + "结束");
    OrgUserDto userDto = commonService.getOrgUserById(model.getOrgId(), model.getCreateBy());
    logger.info("searchOrderContractOne-获取关联合同主体信息开始");
    OrderContract orderContract = this.selectOne(model.getContractId());
    logger.info("searchOrderContractOne-获取关联合同主体信息：" + orderContract.toString() + "结束");
    BeanCopyUtil.copyPropertiesIgnoreNull(orderContract, orderContractVO);
    orderContractVO.setContractCompany(model.getContractCompany());
    orderContractVO.setAttachmentList(attachmentList);
    orderContractVO.setCreateName(userDto.getUserInfo().getName());
    orderContractVO.setDeptName(userDto.getGroupName());
    orderContractVO.setId(id);
    return ResultVOUtil.returnSuccess(orderContractVO);
  }

  @Override
  @Transactional
  public ResponseEnvelope addOrderContract(OrderContractDTO dto) {
    try {
      OrderContract entity = new OrderContract();
      if (CollectionUtils.isEmpty(dto.getAttachmentList())) {
        return ResultVOUtil.returnFail(
            ResultEnum.FILE_NOTNULL.getCode(), ResultEnum.FILE_NOTNULL.getMessage());
      }
      BeanCopyUtil.copyPropertiesIgnoreNull(dto, entity);
      logger.info("addContract-添加合同主体信息开始");
      entity.setOrgId(TokenUtil.getUserInfo().getOrganizationId());
      // 这里自动生成合同业务编码，规则为：HT+时间戳
      String code = NumberUtils.createCode("HT");
      entity.setContractCode(code);
      orderContractMapper.insert(entity);
      logger.info("addContract-添加合同主体信息结束");
      // 合同主体添加成功，进行上传文件的记录保存，并关联到对应合同主体
      // 添加到采购合同表中
      PurcasingContract purcasingContract = new PurcasingContract();
      purcasingContract.setContractId(entity.getId());
      purcasingContract.setContractCompany(dto.getContractCompany());
      purcasingContract.setOrgId(entity.getOrgId());
      purcasingContractMapper.insert(purcasingContract);
      logger.info("addAttachment-添加合同附件信息开始");
      for (OrderAttachmentDTO model : dto.getAttachmentList()) {
        model.setParentId(purcasingContract.getId());
        model.setAtttype(1);
      }
      Integer num1 =
          assetServiceFeign.addBeatchAtta(dto.getAttachmentList(), TokenUtil.getToken()).getCode();
      logger.info("addAttachment-添加合同附件信息结束");
      if (num1 == 0) {
        return ResultVOUtil.returnSuccess();
      } else {
        return ResultVOUtil.returnFail();
      }
    } catch (Exception e) {
      e.printStackTrace();
      return ResultVOUtil.returnFail();
    }
  }

  @Override
  @Transactional
  public ResponseEnvelope updateOrderContract(OrderContractDTO dto) {
    PurcasingContract model = purcasingContractMapper.selectById(dto.getId());
    model.setContractCompany(dto.getContractCompany());
    model.setOrgId(TokenUtil.getUserInfo().getOrganizationId());
    logger.info("updateAttachment-修改采购合同信息开始");
    purcasingContractMapper.updateById(model);
    logger.info("updateAttachment-修改采购合同信息结束");
    logger.info("updateAttachment-修改关联合同主体信息开始");
    OrderContract orderContract = new OrderContract();
    BeanCopyUtil.copyPropertiesIgnoreNull(dto, orderContract);
    orderContract.setId(model.getContractId());
    orderContractMapper.updateById(orderContract);
    logger.info("updateAttachment-修改关联合同主体信息结束");

    // 这里进行合同附件的批量新增操作
    if (CollectionUtils.isEmpty(dto.getAttachmentList())) {
      return ResultVOUtil.returnFail(
          ResultEnum.FILE_NOTNULL.getCode(), ResultEnum.FILE_NOTNULL.getMessage());
    }
    logger.info("updateAttachment-修改合同附件信息开始");
    for (OrderAttachmentDTO model2 : dto.getAttachmentList()) {
      OrderAttachment orderAttachment = new OrderAttachment();
      model2.setParentId(model.getId());
      model2.setAtttype(1);
      BeanCopyUtil.copyPropertiesIgnoreNull(model2, orderAttachment);
      if (model2.getId() != null) {
        orderAttachmentMapper.updateById(orderAttachment);
      } else {
        orderAttachmentMapper.insert(orderAttachment);
      }
    }
    logger.info("updateAttachment-修改合同附件信息结束");
    return ResultVOUtil.returnSuccess();
  }

  @Override
  public List<OrderContractVO> getContractInforList(OrderContractQuery dto) {
    QueryWrapper<OrderContract> queryWrapper = new QueryWrapper<OrderContract>();
    OrderContract entity = new OrderContract();
    queryWrapper.orderByDesc("CREATE_DATE");
    logger.info("getContractInforList-获取导出合同主体id列表信息开始");
    List<Long> paramStr = orderContractMapper.getContractIdList(dto.getContractCompany());
    if (paramStr.size() > 0) {
      queryWrapper.in("ID", paramStr);
    }
    dto.setContractCompany(null);
    BeanCopyUtil.copyPropertiesIgnoreNull(dto, entity);
    entity.setOrgId(TokenUtil.getUserInfo().getOrganizationId());
    queryWrapper.setEntity(entity);
    List<OrderContract> resultList = orderContractMapper.selectList(queryWrapper);
    logger.info("getContractInforList-获取导出合同主体id列表信息结束，获取了" + paramStr.size() + "条数据");
    List<OrderContractVO> contractVOList = new ArrayList<>();
    Integer rownum = 1;
    for (OrderContract a : resultList) {
      // 根据合同id去附件表里获取每个合同对应的附件
      OrgUserDto userDto = commonService.getOrgUserById(a.getOrgId(), a.getCreateBy());
      OrderContractVO contractVO = orderContractMapper.selectContractByCId(a.getId());
      OrderAttachmentDTO attachmentDTO = new OrderAttachmentDTO();
      attachmentDTO.setParentId(a.getId());
      attachmentDTO.setAtttype(1);
      List<Map<String, Object>> fileList =
          assetServiceFeign.selectListByParentId(attachmentDTO, TokenUtil.getToken()).getData();
      String haveFile = fileList.size() > 0 ? "是" : "否";
      OrderContractVO at =
          OrderContractVO.builder()
              .haveFile(haveFile)
              .contractCode(a.getContractCode())
              .contractCompany(contractVO.getContractCompany())
              .contractName(a.getContractName())
              .contractDate(a.getContractDate())
              .contractMoney(a.getContractMoney())
              .id(contractVO.getOrderId())
              .contractTypeName(a.getContractType() == 1 ? "采购合同" : "其他")
              .createDate(a.getCreateDate())
              .createName(userDto.getUserInfo().getName())
              .deptName(userDto.getGroupName())
              .no(rownum)
              .build();
      contractVOList.add(at);
      rownum += 1;
    }
    return contractVOList;
  }

  @Override
  public ResponseEnvelope deleteOrderContract(Long id) {
    // 物理删除送货明细附件表
    logger.info("deleteOrderContract-删除附件表信息开始");
    OrderAttachmentDTO orderAttachmentDTO = new OrderAttachmentDTO();
    orderAttachmentDTO.setParentId(id);
    orderAttachmentDTO.setAtttype(1);
    try {
      assetServiceFeign.deleteAttachmentByParentId(orderAttachmentDTO, TokenUtil.getToken());
    } catch (Exception e) {
      throw new RuntimeException("删除采购合同附件失败，合同id为：" + id);
    }
    logger.info("deleteOrderContract-删除附件表信息结束");
    PurcasingContract entity = new PurcasingContract();
    entity.setId(id);
    QueryWrapper<PurcasingContract> queryWrapper = new QueryWrapper<>();
    queryWrapper.setEntity(entity);
    logger.info("deleteOrderContract-删除采购合同表信息开始");
    entity = purcasingContractMapper.selectOne(queryWrapper);
    // 删除采购合同表
    try {
      purcasingContractMapper.deleteById(id);
      logger.info("deleteOrderContract-删除采购合同表信息结束");
      // 物理删除合同表
      orderContractMapper.deleteById(entity.getContractId());
    } catch (Exception e) {
      throw new RuntimeException("删除采购合同失败，合同id为：" + id);
    }
    return ResultVOUtil.returnSuccess();
  }

  @Override
  public ResponseEnvelope deleteContractList(List<Long> ids) {
    try {
      for (Long id : ids) {
        deleteOrderContract(id);
      }
    } catch (Exception e) {
      throw new RuntimeException("批量删除采购合同失败，报错信息为：" + e.getMessage());
    }
    return ResultVOUtil.returnSuccess();
  }

  public OrderContract selectOne(Long id) {
    OrderContract entity = new OrderContract();
    entity.setId(id);
    QueryWrapper<OrderContract> queryWrapper = new QueryWrapper<>();
    queryWrapper.setEntity(entity);
    return orderContractMapper.selectOne(queryWrapper);
  }
}
