import { Upload, message, Button, Icon } from 'antd';
import React from "react";
import Marpit from '@marp-team/marpit'
import axios from 'axios'


const fs = require('fs');
const props = {
    name: 'file',
    action: 'https://www.mocky.io/v2/5cc8019d300000980a055e76',
    headers: {
        authorization: 'authorization-text',
    },


};

var quizList = new Array();
var data = state.result;
for (int i = 0; i < data.length(); i ++) {
    var j = i;
    var quiz = {};
    if (data.charAt(i) == '$') {
        j = i + 1;
        i = j;
        while (data.charAt(j) != '$') {
            j ++;
        }
        String question = data.substring(i, j);
        j = j + 1;
        i = j;
        quiz["question"] = question;
    }
    var answers = [];
    while (data.charAt(j) != '$' || data.length() == j) {
        var answer = {};
        i = i + 1;
        j = j + 1;
        answer["type"] = data.charAt(i);
        while (data.charAt(j) != '@' || data.charAt(j) != '$' || data.length() == j) {
            j ++;
        }
        String content = data.substring(i + 1, j);
        answer["content"] = content;
        i = j;
        answers.push(answer);
    }
    quiz["answers"] = answers;
    quizList.push(quiz);
}

const marpit = new Marpit();
// 2. Add theme CSS
const theme = `
            /* @theme example */

            section {
              background-color: #369;
              color: #fff;
              font-size: 30px;
              padding: 40px;
            }

        h1,
        h2 {
          text-align: center;
          margin: 0;
        }

        h1 {
          color: #8cf;
        }
        `
marpit.themeSet.default = marpit.themeSet.add(theme)


class MyUpload extends React.Component{
    constructor(props) {
        super(props);
        this.onFileUploaded = props.onFileUploaded;
        this.callback = props.callback;
        this.beforeUpload.bind = this.beforeUpload.bind(this);

    }
    state = {
        file:"",
        result:""
    }

    beforeUpload = (file,fileList) => {
        console.log("FILEEE",file);
        this.setState({
            file:file
        });
    }
    onChange = (info) => {
        // console.log("file!!!:",info.file)

        if (info.file.status !== 'uploading') {
            console.log(info.file, info.fileList);
        }
        if (info.file.status === 'done') {
            // this.convertFile();
            console.log(info.file.name);
            message.success(`${info.file.name} file uploaded successfully`);
            // this.trans();
        } else if (info.file.status === 'error') {
            console.log(info.file.name);
            message.error(`${info.file.name} file upload failed.`);
        }
    }

    onDownload = (file) => {
        this.readFile(this.state.file).then(this.convertText);

        const formData = {
            fileContent: { file },
            userName: 'admin'
        }
        axios
            .post("/", formData, {
                headers: {
                    "Content-Type": "multipart/form-data"
                }
            })
            .then(() => {
                console.log("upload success");
            })
            .catch((error) => {
                console.log("error")
            });
    }

    onPreview = (file) => {
        this.trans();
    }

    readFile=(file)=>{
        return new Promise(function (resolve, reject) {
            const reader = new FileReader();
            console.log("1");
            reader.readAsText(file);
            console.log("2")
            reader.onload = function (e) {
                // let content = e.target.result;
                resolve(this.result);
                console.log("3");
                // console.log(content);
            };
            reader.onerror = function (e) {
                reject(e);
            };
        });
    };

    convertText=(result)=> {
        console.log(result)
        // 3. Render markdown
        const {html, css} = marpit.render(result);
        // 4. Use output in your HTML
        let filestring = `
            <!DOCTYPE html>
            <html><body>
              <style>${css}</style>
              ${html}
            </body></html>
            `
        console.log(filestring)
        ;
        this.setState({
            result: filestring
        },);

        function fakeClick(obj) {
            var ev = document.createEvent("MouseEvents");
            ev.initMouseEvent("click", true, false, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null);
            obj.dispatchEvent(ev);
        }
        function exportRaw(name, data) {
            var urlObject = window.URL || window.webkitURL || window;
            var export_blob = new Blob([data]);
            var save_link = document.createElementNS("http://www.w3.org/1999/xhtml", "a")
            save_link.href = urlObject.createObjectURL(export_blob);
            save_link.download = name;
            fakeClick(save_link);
        }
        exportRaw('filename.html', filestring)
    }

    trans=()=>{
        this.props.callback(this.state.result);
    }


    render(){
        return(
            <div>
                <Upload
                    onChange={this.onChange}
                    beforeUpload={this.beforeUpload}
                    onDownload={this.onDownload}
                    onPreview={this.onPreview}
                    {...props}>

                    <Button>
                        <Icon type="upload" /> Click to Upload
                    </Button>

                </Upload>

            </div>
        )
    }
}

export default MyUpload;