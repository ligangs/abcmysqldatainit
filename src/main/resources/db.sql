-- my_test.abc_charge definition

CREATE TABLE `abc_charge` (
                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '数据表主键',
                              `clinic_id` bigint NOT NULL COMMENT '诊所id',
                              `employ_id` bigint NOT NULL COMMENT '雇员id',
                              `patient_id` bigint NOT NULL COMMENT '患者ID',
                              `amount` decimal(10,2) NOT NULL COMMENT '收费金额',
                              `charge_time` datetime NOT NULL COMMENT '收费时间，格式 2020-01-01 08:39:00',
                              PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='存储收费信息';


-- my_test.abc_clinic definition

CREATE TABLE `abc_clinic` (
                              `id` bigint NOT NULL AUTO_INCREMENT COMMENT '诊所ID',
                              `name` varchar(100) NOT NULL COMMENT '诊所名字',
                              PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- my_test.abc_employee definition

CREATE TABLE `abc_employee` (
                                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '数据表主键(雇员ID)',
                                `name` varchar(100) NOT NULL COMMENT '雇员名字',
                                `type` int NOT NULL COMMENT '雇员类型，1：医生，2：收费员',
                                `clinic_id` bigint DEFAULT NULL COMMENT '诊所id',
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='存储雇员信息';