/*
 * Copyright 2005-2013 shopxx.net. All rights reserved.
 * Support: http://www.shopxx.net
 * License: http://www.shopxx.net/license
 */
package com.fhd.dao.impl;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;

import com.fhd.dao.SnDao;
import com.fhd.entity.Sn;
import com.fhd.util.FreemarkerUtils;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import freemarker.template.TemplateException;

/**
 * Dao - 序列号
 * 
 * @author SHOP++ Team
 * @version 3.0
 */
@Repository("snDaoImpl")
public class SnDaoImpl implements SnDao, InitializingBean {

	private HiloOptimizer productHiloOptimizer;
	private HiloOptimizer orderHiloOptimizer;
	private HiloOptimizer paymentHiloOptimizer;
	private HiloOptimizer refundsHiloOptimizer;
	private HiloOptimizer shippingHiloOptimizer;
	private HiloOptimizer returnsHiloOptimizer;

	@PersistenceContext
	private EntityManager entityManager;
	@Value("${sn.product.prefix}")
	private String productPrefix;
	@Value("${sn.product.maxLo}")
	private int productMaxLo;
	@Value("${sn.order.prefix}")
	private String orderPrefix;
	@Value("${sn.order.maxLo}")
	private int orderMaxLo;
	@Value("${sn.payment.prefix}")
	private String paymentPrefix;
	@Value("${sn.payment.maxLo}")
	private int paymentMaxLo;
	@Value("${sn.refunds.prefix}")
	private String refundsPrefix;
	@Value("${sn.refunds.maxLo}")
	private int refundsMaxLo;
	@Value("${sn.shipping.prefix}")
	private String shippingPrefix;
	@Value("${sn.shipping.maxLo}")
	private int shippingMaxLo;
	@Value("${sn.returns.prefix}")
	private String returnsPrefix;
	@Value("${sn.returns.maxLo}")
	private int returnsMaxLo;

	public void afterPropertiesSet() throws Exception {
		productHiloOptimizer = new HiloOptimizer(Sn.Type.product, productPrefix, productMaxLo);
		orderHiloOptimizer = new HiloOptimizer(Sn.Type.order, orderPrefix, orderMaxLo);
		paymentHiloOptimizer = new HiloOptimizer(Sn.Type.payment, paymentPrefix, paymentMaxLo);
		refundsHiloOptimizer = new HiloOptimizer(Sn.Type.refunds, refundsPrefix, refundsMaxLo);
		shippingHiloOptimizer = new HiloOptimizer(Sn.Type.shipping, shippingPrefix, shippingMaxLo);
		returnsHiloOptimizer = new HiloOptimizer(Sn.Type.returns, returnsPrefix, returnsMaxLo);
	}

	public String generate(Sn.Type type) {
		Assert.notNull(type);
		if (type == Sn.Type.product) {
			return productHiloOptimizer.generate();
		} else if (type == Sn.Type.order) {
			return orderHiloOptimizer.generate();
		} else if (type == Sn.Type.payment) {
			return paymentHiloOptimizer.generate();
		} else if (type == Sn.Type.refunds) {
			return refundsHiloOptimizer.generate();
		} else if (type == Sn.Type.shipping) {
			return shippingHiloOptimizer.generate();
		} else if (type == Sn.Type.returns) {
			return returnsHiloOptimizer.generate();
		}
		return null;
	}

	private long getLastValue(Sn.Type type) {
		String jpql = "select sn from Sn sn where sn.type = :type";
		Sn sn = entityManager.createQuery(jpql, Sn.class).setFlushMode(FlushModeType.COMMIT).setLockMode(LockModeType.PESSIMISTIC_WRITE).setParameter("type", type).getSingleResult();
		long lastValue = sn.getLastValue();
		sn.setLastValue(lastValue + 1);
		entityManager.merge(sn);
		return lastValue;
	}

	/**
	 * 高低位算法
	 */
	private class HiloOptimizer {

		private Sn.Type type;
		private String prefix;
		private int maxLo;
		private int lo;
		private long hi;
		private long lastValue;

		public HiloOptimizer(Sn.Type type, String prefix, int maxLo) {
			this.type = type;
			this.prefix = prefix != null ? prefix.replace("{", "${") : "";
			this.maxLo = maxLo;
			this.lo = maxLo + 1;
		}

		public synchronized String generate() {
			if (lo > maxLo) {
				lastValue = getLastValue(type);
				lo = lastValue == 0 ? 1 : 0;
				hi = lastValue * (maxLo + 1);
			}
			try {
				return FreemarkerUtils.process(prefix, null) + (hi + lo++);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (TemplateException e) {
				e.printStackTrace();
			}
			return String.valueOf(hi + lo++);
		}
	}

}