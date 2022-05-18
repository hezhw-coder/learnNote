# oracle注意事项及优化

 set timing on/off在sqlplus中开启或关闭sql执行时间监测



## 注意事项

- null永远!=null**(例如在where条件后用等号判断某个字段是否为空都会查不出结果)**

- 包含null的表达式都为null**(例如加法运算中有null值,结果肯定为null)**

- 如果集合中含有null，不能使用not in; 但可以使用in

- order by 作用于后面所有的列；desc只作用于离他最近的列

- null在排序中最大

- 组函数（多行函数）自动滤空**(sum、avg等需要用到groupby的函数)**

-  子查询注意事项

  注意的问题：
  1. 括号
  2. 合理的书写风格
  3. 可以在where  select having from后面 都可以使用子查询
  4. 不可以在group by后面使用子查询
  5. 强调from后面的子查询
  6. 主查询和子查询可以不是同一张表；只要子查询返回的结果 主查询可以使用 即可
  7. 一般不在子查询中排序；但在top-n分析问题中，必须对子查询排序
  8. 一般先执行子查询，再执行主查询；但相关子查询例外
  9. 单行子查询只能使用单行操作符；多行子查询只能使用多行操作符
  10.子查询中的null

- rownum永远按照默认的顺序生成,rownum只能使用 < <=; 不能使用> >=

- order by后自动生成临时表(当事务或者会话结束的时候，表中的数据自动删除),rownum使用别名后可以使用>=号(例子分页sql)

  ```sql
  select *
   from 	(select rownum r,e1.*
  	 from (select * from emp order by sal) e1
   	 where rownum <=8
  	)
   where r >=5;
  ```

- delete和truncate的区别

  1. delete逐条删除；truncate先摧毁表，再重建
  2. (根本)delete是DML（可以回滚），truncate是DDL（不可以回滚）
  3. delete不会释放空间 truncate会
   4. delete可以闪回  truncate不可以(flashback)
   5. delete会产生碎片；truncate不会

- 事务的默认的标志

  1. 起始标志：事务中第一条DML语句
  2. 结束标志：提交： 显式 commit
  隐式 正常退出（exit），DDL，DCL
  回滚:   显式 rollback
  隐式 非正常退出，掉电，宕机

- 

## 优化方案

- select后尽量使用列名
-  where解析顺序： 从左右到左解析,因此最先判断的放在最后
- 在group by语句中where与Having都能实现的语句,尽量使用where
- 自连接不适合操作大表,可以使用层级查询
- 尽量不要使用集合运算(union,union all,intersect,minus)
- 尽量使用多表查询，少用子查询

## 知识点

### 海量拷贝数据方案

1. 数据泵(datapump) ---> plsql

2. SQL*Loader

3. (数据仓库)外部表

4. 可传输的表空间



### SQL 的类型

1. DML（data manipulation Language 数据操作语言）: insert  update delete select
2. DDL(Data Definition Language 数据定义语言): create table,alter table,drop table,truncate table
                                                create/drop view,sequence(序列),index,synonym(同义词)
3. DCL(Data Control Language 数据控制语言): grant(授权) revoke（撤销权限）

