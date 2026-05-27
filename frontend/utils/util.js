/**
 * 通用工具函数
 */

/**
 * 格式化日期
 * @param {string|Date} date - 日期
 * @param {string} [format='YYYY-MM-DD'] - 格式
 * @returns {string}
 */
function formatDate(date, format) {
  if (!date) return '';
  const d = typeof date === 'string' ? new Date(date) : date;
  if (isNaN(d.getTime())) return '';

  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');

  if (format === 'MM-DD') return month + '-' + day;
  if (format === 'YYYY年MM月DD日') return year + '年' + month + '月' + day + '日';
  return year + '-' + month + '-' + day;
}

/**
 * 计算天数差
 * @param {string|Date} startDate - 开始日期
 * @param {string|Date} [endDate] - 结束日期，默认今天
 * @returns {number}
 */
function formatDays(startDate, endDate) {
  if (!startDate) return 0;
  const start = typeof startDate === 'string' ? new Date(startDate) : startDate;
  const end = endDate ? (typeof endDate === 'string' ? new Date(endDate) : endDate) : new Date();
  const diff = Math.floor((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24));
  return Math.max(0, diff);
}

/**
 * 安全转为数组
 * @param {*} val - 输入值
 * @returns {Array}
 */
function toSafeArray(val) {
  if (Array.isArray(val)) return val;
  if (val === null || val === undefined) return [];
  if (typeof val === 'string') {
    try {
      const parsed = JSON.parse(val);
      return Array.isArray(parsed) ? parsed : [val];
    } catch (e) {
      return val ? [val] : [];
    }
  }
  return [val];
}

module.exports = { formatDate, formatDays, toSafeArray };
