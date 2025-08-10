const express = require('express');
const mongoose = require('mongoose');
const bodyParser = require('body-parser');

const app = express();
const port = 3000;

app.use(bodyParser.json());

// --- MongoDB Connection ---
mongoose.connect('mongodb://localhost:27017/florisboard', {
  useNewUrlParser: true,
  useUnifiedTopology: true,
  useFindAndModify: false,
});
const db = mongoose.connection;
db.on('error', console.error.bind(console, 'connection error:'));
db.once('open', () => {
  console.log('Connected to MongoDB');
});

// --- Schemas ---
const ClipboardItemSchema = new mongoose.Schema({
    type: String, // 'TEXT', 'IMAGE', 'VIDEO'
    text: String,
    uri: String,
    creationTimestampMs: Number,
    isPinned: Boolean,
    mimeTypes: [String],
    isSensitive: Boolean,
    isRemoteDevice: Boolean,
});

const ClipboardFileInfoSchema = new mongoose.Schema({
    displayName: String,
    size: Number,
    orientation: Number,
    mimeTypes: [String],
});

const ClipboardItem = mongoose.model('ClipboardItem', ClipboardItemSchema);
const ClipboardFileInfo = mongoose.model('ClipboardFileInfo', ClipboardFileInfoSchema);

// --- API Endpoints ---

// Clipboard History
app.get('/clipboard/history', async (req, res) => {
    try {
        const items = await ClipboardItem.find().sort({ creationTimestampMs: -1 });
        res.json(items);
    } catch (err) {
        res.status(500).send(err.message);
    }
});

app.post('/clipboard/history', async (req, res) => {
    try {
        const newItem = new ClipboardItem(req.body);
        const savedItem = await newItem.save();
        res.json(savedItem);
    } catch (err) {
        res.status(500).send(err.message);
    }
});

app.put('/clipboard/history/:id', async (req, res) => {
    try {
        const updatedItem = await ClipboardItem.findByIdAndUpdate(req.params.id, req.body, { new: true });
        res.json(updatedItem);
    } catch (err) {
        res.status(500).send(err.message);
    }
});

app.delete('/clipboard/history/unpinned', async (req, res) => {
    try {
        await ClipboardItem.deleteMany({ isPinned: false });
        res.sendStatus(200);
    } catch (err) {
        res.status(500).send(err.message);
    }
});

app.delete('/clipboard/history/:id', async (req, res) => {
    try {
        await ClipboardItem.findByIdAndDelete(req.params.id);
        res.sendStatus(200);
    } catch (err) {
        res.status(500).send(err.message);
    }
});

app.delete('/clipboard/history', async (req, res) => {
    try {
        await ClipboardItem.deleteMany({});
        res.sendStatus(200);
    } catch (err) {
        res.status(500).send(err.message);
    }
});


// Clipboard Files
app.get('/clipboard/files/:id', async (req, res) => {
    try {
        const fileInfo = await ClipboardFileInfo.findById(req.params.id);
        res.json(fileInfo);
    } catch (err) {
        res.status(500).send(err.message);
    }
});

app.post('/clipboard/files', async (req, res) => {
    try {
        const newFileInfo = new ClipboardFileInfo(req.body);
        const savedFileInfo = await newFileInfo.save();
        res.json(savedFileInfo);
    } catch (err) {
        res.status(500).send(err.message);
    }
});

app.delete('/clipboard/files/:id', async (req, res) => {
    try {
        await ClipboardFileInfo.findByIdAndDelete(req.params.id);
        res.sendStatus(200);
    } catch (err) {
        res.status(500).send(err.message);
    }
});


app.listen(port, () => {
  console.log(`Server listening at http://localhost:${port}`);
});
