import hashlib
import json
from pathlib import Path

# 文件名写着 flower，不能据此认为它是花卉模型。先核对内容摘要和标签。
MODEL = Path("flower-detection.onnx")
KNOWN_IMAGENET_RESNET = "af16a04a6ec48ac494065d4439fe9dea590d337b9ca6dc328160ccf04a217b9c"
raw = MODEL.read_bytes()
pointer = raw.startswith(b"version https://git-lfs.github.com/spec/v1")
if pointer:
    text = raw.decode("utf-8", errors="replace")
    oid = next(line.split("sha256:", 1)[1].strip() for line in text.splitlines() if line.startswith("oid sha256:"))
    identity = {"storage": "git-lfs-pointer", "sha256": oid}
else:
    identity = {"storage": "blob", "sha256": hashlib.sha256(raw).hexdigest(), "bytes": len(raw)}

labels = [line.strip() for line in Path("labels.txt").read_text(encoding="utf-8").splitlines() if line.strip()]
report = {
    "filename": MODEL.name,
    "sha256": identity["sha256"],
    "storage": identity["storage"],
    "matches_known_imagenet_resnet": identity["sha256"] == KNOWN_IMAGENET_RESNET,
    "label_count": len(labels),
    "label_examples": labels[:3] + labels[-3:],
    "flower_specific_model": False,
}
assert report["matches_known_imagenet_resnet"], "文件摘要与已核对的 ImageNet ResNet 不一致，请停止并重新核对"
assert report["label_count"] == 1000, "标签不是 ImageNet 1000 类"

if pointer:
    report["inference"] = "权重未检出，仅凭 Git LFS 对象摘要已能确认它与 ImageNet ResNet 是同一文件。"
else:
    import numpy as np
    import scipy.special
    from PIL import Image
    import onnxruntime as ort

    def preprocess_image(image, resize_size=256, crop_size=224, mean=(0.485, 0.456, 0.406), std=(0.229, 0.224, 0.225)):
        image = image.resize((resize_size, resize_size), Image.BILINEAR)
        width, height = image.size
        left = (width - crop_size) / 2
        top = (height - crop_size) / 2
        image = image.crop((left, top, left + crop_size, top + crop_size))
        array = np.asarray(image).astype(np.float32) / 255.0
        array = (array - np.array(mean, dtype=np.float32)) / np.array(std, dtype=np.float32)
        array = np.transpose(array, (2, 0, 1))
        return array.reshape((1,) + array.shape)

    session = ort.InferenceSession(str(MODEL))
    input_name = session.get_inputs()[0].name
    output_name = session.get_outputs()[0].name
    image = Image.open("flower_test.png").convert("RGB")
    processed = preprocess_image(image).astype(np.float32)
    output = session.run([output_name], {input_name: processed})[0]
    confidence = scipy.special.softmax(output, axis=-1)[0]
    predicted_idx = int(np.argmax(confidence))
    report["predicted_label"] = labels[predicted_idx]
    report["confidence"] = float(confidence[predicted_idx])
    report["inference"] = (
        "softmax 是这一张图片的类别置信度，不是测试准确率。"
        "预测名称来自 ImageNet 1000 类；即使它碰巧是 daisy 这类花卉单词，"
        "同一份权重也会输出鱼类、器物等类别。单张结果不能证明这是花卉专用模型。"
    )

assert report["flower_specific_model"] is False
Path("3.2.4_model_audit.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
print(json.dumps(report, ensure_ascii=False, indent=2))
print("文件名含 flower，但内容摘要与 ImageNet 1000 类 ResNet 相同。不能按文件名声称花卉识别。")
