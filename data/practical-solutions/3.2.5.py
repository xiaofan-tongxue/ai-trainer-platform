import os
import time
import cv2
import numpy as np
import vision.utils.box_utils_numpy as box_utils
import onnxruntime as ort


# 定义预测函数，对模型输出的边界框和置信度进行后处理
def predict(width, height, confidences, boxes, prob_threshold, iou_threshold=0.3, top_k=-1):
    boxes = boxes[0]
    confidences = confidences[0]
    picked_box_probs = []
    picked_labels = []
    for class_index in range(1, confidences.shape[1]):
        probs = confidences[:, class_index]
        mask = probs > prob_threshold
        probs = probs[mask]
        if probs.shape[0] == 0:
            continue
        subset_boxes = boxes[mask, :]
        box_probs = np.concatenate([subset_boxes, probs.reshape(-1, 1)], axis=1)
        box_probs = box_utils.hard_nms(box_probs,
                                       iou_threshold=iou_threshold,
                                       top_k=top_k,
                                       )
        picked_box_probs.append(box_probs)
        picked_labels.extend([class_index] * box_probs.shape[0])
    if not picked_box_probs:
        return np.array([]), np.array([]), np.array([])
    picked_box_probs = np.concatenate(picked_box_probs)
    picked_box_probs[:, 0] *= width
    picked_box_probs[:, 1] *= height
    picked_box_probs[:, 2] *= width
    picked_box_probs[:, 3] *= height
    return picked_box_probs[:, :4].astype(np.int32), np.array(picked_labels), picked_box_probs[:, 4]


# 从标签文件中读取类别名称列表
class_names = [name.strip() for name in open('voc-model-labels.txt').readlines()]

# 创建 ONNX Runtime 的推理会话
ort_session = ort.InferenceSession('version-RFB-320.onnx')

# 获取模型输入的名称
input_name = ort_session.get_inputs()[0].name

# 定义保存检测结果图像的目录路径
result_path = "./detect_imgs_results_onnx"

# 定义置信度阈值
threshold = 0.7
# 定义存储待检测图像的目录路径
path = "imgs"
# 用于统计所有图像中检测到的目标框总数
sum = 0

# 如果保存结果的目录不存在，则创建该目录
if not os.path.exists(result_path):
    os.makedirs(result_path)

# 获取指定目录下的所有文件名称列表
listdir = os.listdir(path)

# 遍历目录下的每个文件
for file_path in listdir:
    # 拼接图像文件的完整路径
    img_path = os.path.join(path, file_path)
    # 使用 OpenCV 读取图像文件
    orig_image = cv2.imread(img_path)
    # 将图像从 BGR 颜色空间转换为 RGB 颜色空间
    image = cv2.cvtColor(orig_image, cv2.COLOR_BGR2RGB)
    # 将图像调整为 320x240 的尺寸
    image = cv2.resize(image, (320, 240))
    # 定义图像归一化的均值数组
    image_mean = np.array([127, 127, 127])
    # 对图像进行归一化处理
    image = (image - image_mean) / 128
    # 将图像的维度从 (H, W, C) 转换为 (C, H, W)
    image = np.transpose(image, [2, 0, 1])
    # 在第一个维度上扩展一个维度
    image = np.expand_dims(image, axis=0)
    # 将图像数据类型转换为 float32 类型
    image = image.astype(np.float32)
    # 记录开始时间
    time_time = time.time()
    # 使用 ONNX Runtime 运行模型
    confidences, boxes = ort_session.run(None, {input_name: image})
    # 打印推理耗时
    print("cost time:{}".format(time.time() - time_time))
    # 后处理
    boxes, labels, probs = predict(orig_image.shape[1], orig_image.shape[0], confidences, boxes, threshold)
    # 遍历每个检测到的目标框
    for i in range(boxes.shape[0]):
        box = boxes[i, :]
        label = f"{class_names[labels[i]]}: {probs[i]:.2f}"
        # 在原始图像上绘制目标框
        cv2.rectangle(orig_image, (box[0], box[1]), (box[2], box[3]), (255, 255, 0), 4)
        # 保存结果图像
        cv2.imwrite(os.path.join(result_path, file_path), orig_image)
    # 累加检测到的目标框数量
    sum += boxes.shape[0]
# 打印目标框总数
print("sum:{}".format(sum))
