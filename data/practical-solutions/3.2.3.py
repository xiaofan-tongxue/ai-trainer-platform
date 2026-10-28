# 导入必要的库
import numpy as np
from PIL import Image
import onnxruntime as ort


# 定义预处理函数，用于将图片转换为模型所需的输入格式
def preprocess(image_path):
    input_shape = (1, 1, 64, 64)    # 模型输入期望的形状 (N, C, H, W)
    img = Image.open(image_path).convert('L')    # 打开图像并转换为灰度图
    img = img.resize((64, 64), Image.Resampling.LANCZOS)    # 调整图像大小到64x64
    img_data = np.array(img, dtype=np.float32)    # 转为numpy数组
    img_data = np.expand_dims(img_data, axis=0)  # 添加 batch 维度
    img_data = np.expand_dims(img_data, axis=1)  # 添加 channel 维度
    assert img_data.shape == input_shape, f"Expected shape {input_shape}, but got {img_data.shape}"
    return img_data


# 定义情感类别与数字标签的映射表
emotion_table = {'neutral':0, 'happiness':1, 'surprise':2, 'sadness':3, 'anger':4, 'disgust':5, 'fear':6, 'contempt':7}

# 加载模型
ort_session = ort.InferenceSession('emotion-ferplus.onnx')

# 加载本地图片并进行预处理
input_data = preprocess('img_test.png')

# 准备输入数据
ort_inputs = {ort_session.get_inputs()[0].name: input_data}

# 运行模型，进行预测
ort_outs = ort_session.run(None, ort_inputs)

# 解码模型输出，找到预测概率最高的情感类别
predicted_label = np.argmax(ort_outs[0])

# 根据预测的标签找到对应的情感名称
predicted_emotion = list(emotion_table.keys())[predicted_label]

# 输出预测的情感
print(f"Predicted emotion: {predicted_emotion}")
